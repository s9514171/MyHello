package codes.chia7712.hellodb.admin;

import codes.chia7712.hellodb.Table;
import codes.chia7712.hellodb.data.BytesUtil;
import codes.chia7712.hellodb.data.Cell;
import codes.chia7712.hellodb.data.CellComparator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

class SimpleAdmin implements Admin {

  private final ConcurrentMap<String, Table> tables = new ConcurrentSkipListMap<>();

  SimpleAdmin(Properties prop) {
    this();
  }

  SimpleAdmin() {
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File("db.txt")));
      String line = null;

      //SimpleTable table = (SimpleTable) openTable("usertable");
      while ((line = br.readLine()) != null) {
        int count = Integer.parseInt(line);
        for (int i = 0; i < count; i++) {
          String tableName = br.readLine();
          createTable(tableName);
          loadDataToTable((SimpleTable) openTable(tableName));
        }
      }
      br.close();
    } catch (IOException ex) {
      System.out.println("MetaData not found!");
    }

  }

  private void loadDataToTable(SimpleTable table) throws FileNotFoundException, IOException {

    BufferedReader br = new BufferedReader(new FileReader(new File(table.name)));
    String line = null;

    //SimpleTable table = (SimpleTable) openTable("usertable");
    while ((line = br.readLine()) != null) {
      int count = Integer.parseInt(line);
      for (int i = 0; i < count; i++) {
        String[] tok = br.readLine().split(", ");
        Cell newCell = Cell.createRowColumnOnly(tok[0].getBytes(), tok[1].getBytes());
        newCell.setDataOffset(Long.parseLong(tok[2]));
        newCell.setDataLength(Integer.parseInt(tok[3]));
        //table.data.put(newCell, newCell);
        table.putCell(newCell);
      }
    }

    //return data.put(newCell, newCell) != null;
  }

  @Override
  public void createTable(String name) throws IOException {
    if (tables.containsKey(name)) {
      throw new IOException(name + " exists");
    }
    tables.computeIfAbsent(name, SimpleTable::new);
  }

  @Override
  public boolean tableExist(String name) throws IOException {
    return tables.containsKey(name);
  }

  @Override
  public void deleteTable(String name) throws IOException {
    if (tables.remove(name) == null) {
      throw new IOException(name + " not found");
    }
  }

  @Override
  public Table openTable(String name) throws IOException {

    Table t = tables.get(name);
    if (t == null) {
      throw new IOException(name + " not found");
    }
    return t;
  }

  @Override
  public List<String> listTables() throws IOException {
    return tables.keySet().stream().collect(Collectors.toList());
  }

  @Override
  public void close() throws IOException {
    // output all tables to disk
    HelloFile taOut = new HelloFile("db.txt");
    taOut.write(BytesUtil.toBytes(tables.size() + "\n"));
    List<String> ta = listTables();
    for (String item : ta) {
      taOut.write((item + "\n").getBytes());

      SimpleTable cellTable = (SimpleTable) openTable(item);
      cellTable.outputCellToDisk();
    }
    taOut.close();
    /*
    for (Map.Entry<Cell, Cell> entry : data.entrySet()) {
      meta.write(BytesUtil.toBytes(new String(entry.getValue().getRowArray()) + ", " + new String(entry.getValue().getColumnArray()) + ", " + entry.getValue().getDataOffset() + ", " + entry.getValue().getDataLength() + "\n"));
      //System.out.println("Offset: "+entry.getValue().getDataOffset());
    }*/
  }

  private static class SimpleTable implements Table {

    private static final CellComparator CELL_COMPARATOR = new CellComparator();
    private final ConcurrentNavigableMap<Cell, Cell> data = new ConcurrentSkipListMap<>(CELL_COMPARATOR);
    private final String name;
    private final HelloFile file = new HelloFile();

    SimpleTable(final String name) {
      this.name = name;
    }

    private void putCell(Cell cell) {
      data.put(cell, cell);
    }

    private void outputCellToDisk() throws IOException {
      // output cells to disk
      HelloFile cellOut = new HelloFile(this.name);
      cellOut.write((data.size() + "\n").getBytes());
      for (Map.Entry<Cell, Cell> entry : data.entrySet()) {
        cellOut.write((new String(entry.getValue().getRowArray()) + ", " + new String(entry.getValue().getColumnArray()) + ", " + entry.getValue().getDataOffset() + ", " + entry.getValue().getDataLength() + "\n").getBytes());
        //System.out.println("Offset: "+entry.getValue().getDataOffset());
      }
    }

    @Override
    public boolean insert(Cell cell) throws IOException {

      Cell newCell = Cell.createRowColumnOnly(cell.getRowArray(), cell.getColumnArray());
      newCell.setDataLength(cell.getValueLength());
      newCell.setDataOffset(file.write(cell.getValueArray()));

      return data.put(newCell, newCell) != null;

    }

    @Override
    public void delete(byte[] row) throws IOException {
      Cell rowOnlyCell = Cell.createRowOnly(row);
      for (Map.Entry<Cell, Cell> entry : data.tailMap(rowOnlyCell).entrySet()) {
        if (CellComparator.compareRow(entry.getKey(), rowOnlyCell) != 0) {
          return;
        } else {
          data.remove(entry.getKey());
        }
      }
    }

    @Override
    public Iterator<Cell> get(byte[] row) throws IOException {
      Cell rowOnlyCell = Cell.createRowOnly(row);
      List<Cell> rval = new ArrayList<>();
      for (Map.Entry<Cell, Cell> entry : data.tailMap(rowOnlyCell).entrySet()) {
        if (CellComparator.compareRow(entry.getKey(), rowOnlyCell) != 0) {
          break;
        } else {
          byte[] b = new byte[entry.getValue().getDataLength()];
          file.read(b, entry.getValue().getDataOffset());
          Cell newCell = Cell.createCell(row, entry.getValue().getColumnArray(), b);
          rval.add(newCell);
        }
      }
      return rval.iterator();
    }

    @Override
    public Optional<Cell> get(byte[] row, byte[] column) throws IOException {
      if (Optional.ofNullable(data.get(Cell.createRowColumnOnly(row, column))).isPresent()) {
        Cell cell = data.get(Cell.createRowColumnOnly(row, column));
        byte[] b = new byte[cell.getDataLength()];
        file.read(b, cell.getDataOffset());
        return Optional.ofNullable(Cell.createCell(cell.getRowArray(), cell.getColumnArray(), b));
      }
      return Optional.ofNullable(data.get(Cell.createRowColumnOnly(row, column)));

    }

    @Override
    public boolean delete(byte[] row, byte[] column) throws IOException {
      return data.remove(Cell.createRowColumnOnly(row, column)) != null;
    }

    @Override
    public boolean insertIfAbsent(Cell cell) throws IOException {
      Cell newCell = Cell.createRowColumnOnly(cell.getRowArray(), cell.getColumnArray());
      newCell.setDataLength(cell.getValueLength());
      newCell.setDataOffset(file.write(cell.getValueArray()));

      return data.putIfAbsent(newCell, newCell) == null;
    }

    @Override
    public void close() throws IOException {
      //nothing
    }

    @Override
    public String getName() {
      return name;
    }

  }

}