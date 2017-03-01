/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codes.chia7712.hellodb.admin;

import codes.chia7712.hellodb.Table;
import codes.chia7712.hellodb.data.BytesUtil;
import codes.chia7712.hellodb.data.Cell;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lab
 */
public class entry {

  public static void main(String[] args) {
    try {
      SimpleAdmin admin = new SimpleAdmin();
      try {
        //admin.createTable("usertable");
        admin.createTable("usertable");
      } catch (IOException ex) {
        System.out.println(ex.toString());
      }
      //Table table2 = new SimpleAdmin().openTable("usertable");
      Table table = admin.openTable("usertable");
      
      //for (int i = 0 ;i<10000;i++) {
        table.insert(Cell.createCell(("row" + 1).getBytes(), ("col" + 2).getBytes(), ("val" + 2).getBytes()));
        table.insert(Cell.createCell(("row" + 3).getBytes(), ("col" + 2).getBytes(), ("val" + 2).getBytes()));
      //}
      
      /*byte[] t1 = BytesUtil.toBytes("row");
      byte[] t2 = BytesUtil.toBytes("col");
      byte[] t3 = BytesUtil.toBytes("val");
      System.out.println(table.insert(Cell.createCell(t2, t1, t3)));
      System.out.println(table.insert(Cell.createCell(t1, t2, t3)));
      System.out.println(table.insert(Cell.createCell(t3, t2, t3)));
      System.out.println(table.insert(Cell.createCell(t1, t3, t3)));
      System.out.println(table.insert(Cell.createCell(t1, t1, t3)));
      System.out.println(table.insert(Cell.createCell(t1, t3, t1)));
      System.out.println(table.insert(Cell.createCell(t3, t1, t3)));
      System.out.println(new String(table.get(t1, t2).get().getValueArray()));*/
      //admin.createTable("leeTable");
      //Table table2= admin.openTable("leeTable");
      //table2.insert(Cell.createCell(t1, t3, t1));
      //System.out.println("123123123 : "+table2.insert(Cell.createCell(t3, t1, t3)));
      //System.out.println("123123123 : "+table2.insert(Cell.createCell(t3, t1, t3)));
      table.close();
      //table2.close();
      admin.close();
      //System.out.println(t3);
    } catch (IOException ex) {
      Logger.getLogger(entry.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}