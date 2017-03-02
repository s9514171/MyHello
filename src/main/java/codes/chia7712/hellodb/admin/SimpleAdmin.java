package codes.chia7712.hellodb.admin;

import codes.chia7712.hellodb.Table;
import codes.chia7712.hellodb.data.Cell;
import codes.chia7712.hellodb.data.CellComparator;
import codes.chia7712.hellodb.data.SimpleCell;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
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
      RandomAccessFile raf = new RandomAccessFile("db.txt", "r");
      raf.seek(0);
      int n = raf.readInt();
      byte[] b;
      String tableName;
      for (int i = 0; i < n; i++) {
        b = new byte[raf.readInt()];
        raf.read(b);
        tableName = new String(b);
        createTable(tableName);
        loadDataToTable((SimpleTable) openTable(tableName));
      }
      raf.close();
    } catch (IOException ex) {
      System.out.println("MetaData not found!");
    }

  }

  private void loadDataToTable(SimpleTable table) throws FileNotFoundException, IOException {

    RandomAccessFile raf = new RandomAccessFile(table.name, "r");
    raf.seek(0);
    byte[] r, c;
    int n = raf.readInt();
    for (int i = 0; i < n; i++) {
      r = new byte[raf.readInt()];
      raf.read(r);
      c = new byte[raf.readInt()];
      raf.read(c);
      SimpleCell newCell = (SimpleCell) Cell.createRowColumnOnly(r, c);
      newCell.setDataOffset(raf.readLong());
      newCell.setDataLength(raf.readInt());
      table.putCell(newCell);
    }
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
    taOut.writeInt(tables.size());
    List<String> ta = listTables();
    byte[] b;
    for (String item : ta) {
      b = item.getBytes();
      taOut.writeInt(b.length);
      taOut.write(b);
      SimpleTable cellTable = (SimpleTable) openTable(item);
      cellTable.outputCellToDisk();
    }
    taOut.close();
  }

  private static class SimpleTable implements Table {

    private static final CellComparator CELL_COMPARATOR = new CellComparator();
    private final ConcurrentNavigableMap<Cell, Cell> data = new ConcurrentSkipListMap<>(CELL_COMPARATOR);
    private final String name;
    private static final HelloFile file = new HelloFile(); // write data

    SimpleTable(final String name) {
      this.name = name;
    }

    private void putCell(Cell cell) {
      data.put(cell, cell);
    }

    private void outputCellToDisk() throws IOException {
      // output cells to disk

      HelloFile cellOut = new HelloFile(this.name);
      cellOut.writeInt(data.size());

      for (Map.Entry<Cell, Cell> entry : data.entrySet()) {
        cellOut.writeInt(entry.getValue().getRowLength());
        cellOut.write(entry.getValue().getRowArray());
        cellOut.writeInt(entry.getValue().getColumnLength());
        cellOut.write(entry.getValue().getColumnArray());
        cellOut.writeLong(((SimpleCell)entry.getValue()).getDataOffset());
        cellOut.writeInt(((SimpleCell)entry.getValue()).getDataLength());
      }

    }

    @Override
    public boolean insert(Cell cell) throws IOException {
      SimpleCell newCell = (SimpleCell)Cell.createRowColumnOnly(Arrays.copyOfRange(cell.getRowArray(), cell.getRowOffset(), cell.getRowOffset() + cell.getRowLength()),
              Arrays.copyOfRange(cell.getColumnArray(), cell.getColumnOffset(), cell.getColumnOffset() + cell.getColumnLength()));
      newCell.setDataLength(cell.getValueLength());
      newCell.setDataOffset(file.write(Arrays.copyOfRange(cell.getValueArray(), cell.getValueOffset(), cell.getValueOffset() + cell.getValueLength())));

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
          byte[] b = new byte[((SimpleCell)entry.getValue()).getDataLength()];
          file.read(b, ((SimpleCell)entry.getValue()).getDataOffset());
          Cell newCell = Cell.createCell(row, entry.getValue().getColumnArray(), b);
          rval.add(newCell);
        }
      }
      return rval.iterator();
    }

    @Override
    public Optional<Cell> get(byte[] row, byte[] column) throws IOException {
      if (Optional.ofNullable(data.get(Cell.createRowColumnOnly(row, column))).isPresent()) {
        SimpleCell cell = (SimpleCell)data.get(Cell.createRowColumnOnly(row, column));
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
      SimpleCell newCell = (SimpleCell)Cell.createRowColumnOnly(Arrays.copyOfRange(cell.getRowArray(), cell.getRowOffset(), cell.getRowOffset() + cell.getRowLength()),
              Arrays.copyOfRange(cell.getColumnArray(), cell.getColumnOffset(), cell.getColumnOffset() + cell.getColumnLength()));
      if (data.putIfAbsent(newCell, newCell) == null) {
        newCell.setDataLength(cell.getValueLength());
        newCell.setDataOffset(file.write(Arrays.copyOfRange(cell.getValueArray(), cell.getValueOffset(), cell.getValueOffset() + cell.getValueLength())));
        return true;
      }

      return false;
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
