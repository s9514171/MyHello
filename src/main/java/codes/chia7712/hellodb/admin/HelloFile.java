/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package codes.chia7712.hellodb.admin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lab
 */
public class HelloFile {

  private RandomAccessFile raf;
  private long offset;
  private static final Object syncObject = new Object();

  public long write(byte[] b) throws IOException {
    synchronized (syncObject) {
      raf.seek(this.offset);
      raf.write(b);
      this.offset += b.length;
      return this.offset - b.length;
    }

  }

  public void read(byte[] b, long off) {
    try {
      raf.seek(off);
      raf.readFully(b);
    } catch (IOException ex) {
      Logger.getLogger(HelloFile.class.getName()).log(Level.SEVERE, null, ex);
      //System.out.println("errorRead");
    }
  }

  public long getLength() throws IOException {
    return raf.length();
  }

  public HelloFile() {
    try {
      raf = new RandomAccessFile("data.txt", "rw");
      this.offset = raf.length();
    } catch (FileNotFoundException ex) {
      Logger.getLogger(HelloFile.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(HelloFile.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public HelloFile(String str) {
    try {
      raf = new RandomAccessFile(str, "rw");
      // for metadata offset = 0
      this.offset = 0;//raf.length();
    } catch (FileNotFoundException ex) {
      Logger.getLogger(HelloFile.class.getName()).log(Level.SEVERE, null, ex);
    } /*catch (IOException ex) {
    Logger.getLogger(HelloFile.class.getName()).log(Level.SEVERE, null, ex);
    }*/
  }

  public void close() throws IOException {
    raf.close();
  }
}
