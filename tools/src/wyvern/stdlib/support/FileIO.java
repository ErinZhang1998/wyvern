package wyvern.stdlib.support;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/** New imports! **/
import java.io.RandomAccessFile;

public class FileIO {
    public static final FileIO file = new FileIO();

    public PrintWriter openForAppend(String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path, true);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        return new PrintWriter(bufferedWriter);
    }

    public BufferedReader openForRead(String path) throws IOException {
        FileReader fileReader = new FileReader(path);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        return new BufferedReader(bufferedReader);
    }

    public String readFileIntoString(BufferedReader br) throws IOException {
        String line = "";
        String message = "";
        while ((line = br.readLine()) != null) {
            message += line;
        }
        return message;
    }

    public void writeStringIntoFile(String content, String filename) throws IOException {
        File file = new File(filename + "-files/" + filename + "" + System.currentTimeMillis() + ".txt");
        file.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(file);
        writer.println(content);
        writer.close();
    }

    /** NEW METHODS ADDED FOR IO LIBRARY **/
    
    public File createNewFile(String pathname) {
        return new File(pathname);
    }
    
    /** Naming conventions a bit shady but won't conflict with previously defined methods **/
    public BufferedWriter openBWForWrite(Object f) throws IOException {
        return new BufferedWriter(new FileWriter((File) f));
    }
    
    public BufferedWriter openBWForAppend(Object f) throws IOException {
        return new BufferedWriter(new FileWriter((File) f, true));
    }
    
    public BufferedReader openBRForRead(Object f) throws IOException {
        return new BufferedReader(new FileReader((File) f));
    }
    
    /* Used for both append and write, since both are BufferedWriter */
    public void writeString(BufferedWriter bw, String s) throws IOException {
        bw.write(s, 0, s.length());
    }
    
    public String readLineFromFile(BufferedReader br) throws IOException {
        return br.readLine();
    }
    
    public boolean isNull(Object o) {
        return o == null;
    }
    
    public String readFullyFile(BufferedReader br) throws IOException {
        String next = br.readLine();
        if (next == null) {
            return "";
        } else {
            String acc = next;
            next = br.readLine();
            while (next != null) {
                acc += "\n" + next;
                next = br.readLine();
            }
            return acc;
        }
    }
    
    public int readCharFromFile(BufferedReader br) throws IOException {
        return br.read();
    }
    
    public void closeWriter(BufferedWriter bw) throws IOException {
        bw.close();
    }
    
    public void closeReader(BufferedReader br) throws IOException {
        br.close();
    }
    
    /** functionality for RandomAccessFile **/
    
    public RandomAccessFile makeRandomAccessFile(Object f, String mode) throws IOException {
        return new RandomAccessFile((File) f, mode);
    }
    
    public void closeRandomAccessFile(RandomAccessFile r) throws IOException {
        //uhh so consider trying to copy the whole file to a writer at the end
        r.close();
    }
    
    public void writeUTFRandomAccess(RandomAccessFile r, String s) throws IOException {
        r.writeUTF(s);
    }
    
    public void writeStringRandomAccess(RandomAccessFile r, String s) throws IOException {
        //r.writeChars(s);
        for (int i = 0; i < s.length(); i++) {
            r.writeChar(s.charAt(i));
            //r.seek(r.getFilePointer() - 1);
            //System.out.println(r.getFilePointer());
        }
    }
    
    public String readUTFRandomAccess(RandomAccessFile r) throws IOException {
        return r.readUTF();
    }
    
    public String readLineRandomAccess(RandomAccessFile r) throws IOException {
        return r.readLine();
    }
    
    public long accessFilePointer(RandomAccessFile r) throws IOException {
        return r.getFilePointer();
    }
    
    //check if long actually works here?
    public void seekFilePointer(RandomAccessFile r, long pos) throws IOException {
        r.seek(pos);
    }
    
    //seeks relative to current position
    public void seekRelativeFilePointer(RandomAccessFile r, long offset) throws IOException {
        //check negative offset
        r.seek(r.getFilePointer() + offset);
    }
    
    public long getRandomAccessFileLength(RandomAccessFile r) throws IOException {
        return r.length();
    }
    
    /** way more read/write methods to add here **/
    


}
