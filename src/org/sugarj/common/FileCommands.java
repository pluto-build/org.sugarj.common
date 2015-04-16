package org.sugarj.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * Provides methods for doing stuff with files.
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class FileCommands {

  public final static boolean DO_DELETE = true;

  public final static String TMP_DIR;
  static {
    try {
      TMP_DIR = File.createTempFile("tmp", "").getParent();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 
   * @param suffix
   *          without dot "."
   * @return
   * @throws IOException
   */
  public static Path newTempFile(String suffix) throws IOException {
    File f = File.createTempFile("sugarj", suffix == null || suffix.isEmpty() ? suffix : "." + suffix);
    final Path p = new AbsolutePath(f.getAbsolutePath());

    return p;
  }

  public static void deleteTempFiles(Path file) throws IOException {
    if (file == null)
      return;

    String parent = file.getFile().getParent();

    if (parent == null)
      return;
    else if (parent.equals(TMP_DIR))
      delete(file);
    else
      deleteTempFiles(new AbsolutePath(parent));
  }

  public static void delete(Path file) throws IOException {
    if (file == null)
      return;

    if (file.getFile().listFiles() != null)
      for (File f : file.getFile().listFiles())
        FileCommands.delete(new AbsolutePath(f.getPath()));

    file.getFile().delete();
  }

  public static void copyFile(Path from, Path to, CopyOption... options) throws IOException {
    Set<CopyOption> optSet = new HashSet<>();
    for (CopyOption o : options)
      optSet.add(o);
    optSet.add(StandardCopyOption.REPLACE_EXISTING);

    Files.copy(from.getFile().toPath(), to.getFile().toPath(), optSet.toArray(new CopyOption[optSet.size()]));
  }

  public static void copyFile(InputStream in, OutputStream out) throws IOException {
    int len;
    byte[] b = new byte[1024];

    while ((len = in.read(b)) > 0)
      out.write(b, 0, len);
  }

  /**
   * Beware: one must not rename SDF files since the filename and the module
   * name needs to coincide. Instead generate a new file which imports the other
   * SDF file.
   * 
   * @param file
   * @param content
   * @throws IOException
   */
  public static void writeToFile(Path file, String content) throws IOException {
    FileCommands.createFile(file);
    FileOutputStream fos = new FileOutputStream(file.getFile());
    fos.write(content.getBytes());
    fos.close();
  }
  
  public static void writeLinesFile(Path file, List<String> lines) throws IOException {
    FileCommands.createFile(file);
    BufferedWriter writer = new BufferedWriter(new FileWriter(file.getFile()));
    Iterator<String> iter = lines.iterator();
    while(iter.hasNext()) {
      writer.write(iter.next());
      if (iter.hasNext()) {
      writer.write("\n");
      }
    }
    writer.flush();
    writer.close();
  }

  public static void appendToFile(Path file, String content) throws IOException {
    createFile(file);
    FileOutputStream fos = new FileOutputStream(file.getFile(), true);
    fos.write(content.getBytes());
    fos.close();
  }

  public static byte[] readFileAsByteArray(Path file) throws IOException {
    return readFileAsByteArray(file.getFile());
  }

  public static byte[] readFileAsByteArray(File file) throws IOException {
    return Files.readAllBytes(file.toPath());
  }

  public static String readFileAsString(File file) throws IOException {
    return readFileAsString(new AbsolutePath(file.getAbsolutePath()));
  }

  // from http://snippets.dzone.com/posts/show/1335
  // Author: http://snippets.dzone.com/user/daph2001
  public static String readFileAsString(Path filePath) throws IOException {
    StringBuilder fileData = new StringBuilder(1000);
    BufferedReader reader = new BufferedReader(new FileReader(filePath.getFile()));
    char[] buf = new char[1024];
    int numRead = 0;
    while ((numRead = reader.read(buf)) != -1)
      fileData.append(buf, 0, numRead);

    reader.close();
    return fileData.toString();
  }
  
  public static List<String> readFileLines(Path filePath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filePath.getFile()));
    List<String> lines = new ArrayList<>();
    String temp;
    while((temp = reader.readLine()) != null) {
      lines.add(temp);
    }
    reader.close();
    return lines;
  }

  public static String readStreamAsString(InputStream in) throws IOException {
    StringBuilder fileData = new StringBuilder(1000);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    char[] buf = new char[1024];
    int numRead = 0;
    while ((numRead = reader.read(buf)) != -1)
      fileData.append(buf, 0, numRead);

    reader.close();
    return fileData.toString();
  }

  public static String fileName(URL url) {
    return fileName(new AbsolutePath(url.getPath()));
  }

  public static String fileName(URI uri) {
    return fileName(new AbsolutePath(uri.getPath()));
  }

  public static String fileName(Path file_doof) {
    return fileName(toCygwinPath(file_doof.getAbsolutePath()));
  }

  public static String fileName(String file) {
    int index = file.lastIndexOf(File.separator);

    if (index >= 0)
      file = file.substring(index + 1);

    index = file.lastIndexOf(".");

    if (index > 0)
      file = file.substring(0, index);

    return file;
  }

  public static RelativePath[] listFiles(Path p) {
    return listFiles(p, null);
  }

  public static RelativePath[] listFiles(Path p, FileFilter filter) {
    File[] files = p.getFile().listFiles(filter);
    RelativePath[] paths = new RelativePath[files.length];

    for (int i = 0; i < files.length; i++)
      paths[i] = new RelativePath(p, files[i].getName());

    return paths;
  }

  public static List<RelativePath> listFilesRecursive(Path p) {
    return listFilesRecursive(p, null);
  }

  public static List<RelativePath> listFilesRecursive(Path p, final FileFilter filter) {
    File[] files = p.getFile().listFiles();
    if (files == null)
      return Collections.emptyList();

    List<RelativePath> paths = new ArrayList<>();

    for (int i = 0; i < files.length; i++) {
      RelativePath rel = new RelativePath(p, files[i].getName());
      if (filter == null || filter.accept(files[i]))
        paths.add(rel);
      if (files[i].isDirectory())
        for (Path sub : listFilesRecursive(rel, filter))
          paths.add(getRelativePath(rel, sub));
    }

    return paths;
  }

  /**
   * Finds the given file in the given list of paths.
   * 
   * @param filename
   *          relative filename.
   * @param paths
   *          list of possible paths to filename
   * @return full file path to filename or null
   */
  @Deprecated
  public static String findFile(String filename, List<String> paths) {
    return findFile(filename, paths.toArray(new String[] {}));
  }

  /**
   * Finds the given file in the given list of paths.
   * 
   * @param filename
   *          relative filename.
   * @param paths
   *          list of possible paths to filename
   * @return full file path to filename or null
   */
  @Deprecated
  public static String findFile(String filename, String... paths) {
    for (String path : paths) {
      File f = new File(path + File.separator + filename);
      if (f.exists())
        return f.getAbsolutePath();
    }

    return null;
  }

  public static Path newTempDir() throws IOException {
    final File f = File.createTempFile("SugarJ", "");
    // need to delete the file, but want to reuse the filename
    f.delete();
    f.mkdir();
    final Path p = new AbsolutePath(f.getAbsolutePath());

    return p;
  }

  public static Path tryNewTempDir() {
    try {
      return newTempDir();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void prependToFile(Path file, String head) throws IOException {
    Path tmp = newTempFile("");
    file.getFile().renameTo(tmp.getFile());

    FileInputStream in = new FileInputStream(tmp.getFile());
    FileOutputStream out = new FileOutputStream(file.getFile());

    out.write(head.getBytes());

    int len;
    byte[] b = new byte[1024];

    while ((len = in.read(b)) > 0)
      out.write(b, 0, len);

    in.close();
    out.close();
    delete(tmp);
  }

  public static void createFile(Path file) throws IOException {
    File f = file.getFile();
    if (f.getParentFile().exists() || f.getParentFile().mkdirs())
      f.createNewFile();
  }

  /**
   * Create file with name deduced from hash in dir.
   * 
   * @param dir
   * @param hash
   * @return
   * @throws IOException
   */
  public static Path createFile(Path dir, int hash) throws IOException {
    Path p = new RelativePath(dir, hashFileName("sugarj", hash));
    createFile(p);
    return p;
  }

  public static void createDir(Path dir) throws IOException {
    boolean isMade = dir.getFile().mkdirs();
    boolean exists = dir.getFile().exists();
    if (!isMade && !exists)
      throw new IOException("Failed to create the directories\n" + dir);
  }

  /**
   * Create directory with name deduced from hash in dir.
   * 
   * @param dir
   * @param hash
   * @return
   * @throws IOException
   */
  public static Path createDir(Path dir, int hash) throws IOException {
    Path p = new RelativePath(dir, hashFileName("SugarJ", hash));
    createDir(p);
    return p;
  }

  /**
   * Ensures that a path is suitable for a cygwin command line.
   */
  public static String toCygwinPath(String filepath) {
    // XXX hacky

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      filepath = filepath.replace("\\", "/");
      filepath = filepath.replace("/C:/", "/cygdrive/C/");
      filepath = filepath.replace("C:/", "/cygdrive/C/");
    }

    return filepath;
  }

  /**
   * Ensure that a path is suitable for a windows command line
   */
  public static String toWindowsPath(String filepath) {
    // XXX hacky

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      filepath = filepath.replace("/cygdrive/C", "C:");
      filepath = filepath.replace("/C:", "C:");
      filepath = filepath.replace("/", "\\");
    }

    return filepath;
  }

  /**
   * checks whether f1 was modified after f2.
   * 
   * @return true iff f1 was modified after f2.
   */
  public static boolean isModifiedLater(Path f1, Path f2) {
    return f1.getFile().lastModified() > f2.getFile().lastModified();
  }

  public static boolean fileExists(Path file) {
    return file != null && file.getFile().exists() && file.getFile().isFile();
  }

  public static boolean exists(Path file) {
    return file != null && file.getFile().exists();
  }

  public static boolean exists(URI file) {
    return new File(file).exists();
  }

  public static String hashFileName(String prefix, int hash) {
    return prefix + (hash < 0 ? "1" + Math.abs(hash) : "0" + hash);
  }

  public static String hashFileName(String prefix, Object o) {
    return hashFileName(prefix, o.hashCode());
  }

  public static String getExtension(Path infile) {
    return getExtension(infile.getFile());
  }

  public static String getExtension(File infile) {
    return getExtension(infile.getName());
  }

  public static String getExtension(String infile) {
    int i = infile.lastIndexOf('.');

    if (i > 0)
      return infile.substring(i + 1, infile.length());

    return null;
  }

  public static String dropExtension(String file) {
    int i = file.lastIndexOf('.');

    if (i > 0)
      return file.substring(0, i);

    return file;
  }

  public static String dropDirectory(Path p) {
    String ext = getExtension(p);
    if (ext == null)
      return fileName(p);
    else 
      return fileName(p) + "." + getExtension(p);
  }

  public static AbsolutePath replaceExtension(AbsolutePath p, String newExtension) {
    return p.replaceExtension(newExtension);
  }
  public static RelativePath replaceExtension(RelativePath p, String newExtension) {
    return p.replaceExtension(newExtension);
  }

  public static Path addExtension(Path p, String newExtension) {
    if (p instanceof RelativePath)
      return new RelativePath(((RelativePath) p).getBasePath(), ((RelativePath) p).getRelativePath() + "." + newExtension);
    return new AbsolutePath(p.getAbsolutePath() + "." + newExtension);
  }

  public static AbsolutePath addExtension(AbsolutePath p, String newExtension) {
    return new AbsolutePath(p.getAbsolutePath() + "." + newExtension);
  }

  public static RelativePath dropFilename(RelativePath file) {
    return new RelativePath(file.getBasePath(), dropFilename(file.getRelativePath()));
  }
  
  public static AbsolutePath dropFilename(Path file) {
    return new AbsolutePath(dropFilename(file.getAbsolutePath()));
  }

  public static String dropFilename(String file) {
    int i = file.lastIndexOf(File.separator);
    if (i > 0)
      return file.substring(0, i);

    return "";
  }

  public static byte[] fileHash(Path file) throws IOException {
    // http://www.codejava.net/coding/how-to-calculate-md5-and-sha-hash-values-in-java
    try (FileInputStream inputStream = new FileInputStream(file.getFile())) {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        
        byte[] bytesBuffer = new byte[1024];
        int bytesRead = -1;
 
        while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
            digest.update(bytesBuffer, 0, bytesRead);
        }
 
        byte[] hashedBytes = digest.digest();
 
        return hashedBytes;
    } catch (NoSuchAlgorithmException | IOException ex) {
        return null;
    }
  }

  public static byte[] tryFileHash(Path file) {
    try {
      return FileCommands.fileHash(file);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static boolean isEmptyFile(Path prog) throws IOException {
    FileInputStream in = null;

    try {
      in = new FileInputStream(prog.getFile());
      if (in.read() == -1)
        return true;
      return false;
    } finally {
      if (in != null)
        in.close();
    }
  }

  // cai 27.09.12
  // convert path-separator to that of the OS
  // so that strategoXT doesn't prepend ./ to C:/foo/bar/baz.
  public static String nativePath(String path) {
    return path.replace('/', File.separatorChar);
  }

  public static RelativePath getRelativePath(Path base, Path fullPath) {
    if (fullPath instanceof RelativePath && ((RelativePath) fullPath).getBasePath().equals(base))
      return (RelativePath) fullPath;

    String baseS = base.getAbsolutePath();
    String fullS = fullPath.getAbsolutePath();

    if (fullS.startsWith(baseS))
      return new RelativePath(base, fullS.substring(baseS.length()));

    return null;
  }

  public static Path copyFile(Path from, Path to, Path file, CopyOption... options) {
    RelativePath p = getRelativePath(from, file);
    if (p == null)
      return null;

    RelativePath target = new RelativePath(to, p.getRelativePath());
    if (!FileCommands.exists(p))
      return target;
    try {
      copyFile(p, target, options);
      return target;
    } catch (IOException e) {
      e.printStackTrace();
      return target;
    }
  }

  public static String tryGetRelativePath(Path p) {
    if (p instanceof RelativePath)
      return ((RelativePath) p).getRelativePath();
    return p.getAbsolutePath();
  }
  
  public static Path getRessourcePath(Class<?> clazz) throws URISyntaxException {
    String className = clazz.getName();
    URL url = clazz.getResource(className.substring(className.lastIndexOf(".") + 1) + ".class");
    String path = url == null ? null : url.getPath();
    if (path == null)
      return null;
    
    // remove URL leftovers
    if (path.startsWith("file:")) {
      path = path.substring("file:".length());
    }

    // is the class file inside a jar?
    if (path.contains(".jar!")) {
      path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
    }

    // have we found the class file?
    if (AbsolutePath.acceptable(path))
      return new AbsolutePath(path);
    
    return null;
  }
}
