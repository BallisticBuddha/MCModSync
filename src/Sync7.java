import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.net.URLClassLoader;

import org.apache.commons.lang3.SystemUtils;


public class Sync7 {

	private String folderName;
	private Set<String> newConfigs = new HashSet<String>();
	private char directorySeparator = '/';
	private String mcFolder;
	private boolean verbose = false;
	
	public char getDirectorySeparator() { return directorySeparator; }
	public String getMcFolder() { return mcFolder; }
	public void enableVerbose(){ verbose = true; }
	
	public Sync7(String tempFolder) throws IOException{
		this.folderName = tempFolder;
		this.mcFolder = "";
		
		if (SystemUtils.IS_OS_WINDOWS){
			mcFolder = System.getenv("APPDATA")+"\\.minecraft";
			directorySeparator = '\\';
		} else if (SystemUtils.IS_OS_MAC_OSX){
			mcFolder = System.getProperty("user.home")+"/Library/Application Support/minecraft";
		} else{
			mcFolder = System.getProperty("user.home")+"/.minecraft";
		}
	}
	
	public boolean moveToBackup(String mcFolder) throws IOException{
		Path mods = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"mods");
		//Path coremods = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"coremods");
		Path config = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"config");	
		Path backupDir = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"backup");
		
		Path modsBackup = FileSystems.getDefault().getPath(backupDir.toAbsolutePath().toString()+directorySeparator+"mods");
	    //Path coremodsBackup = FileSystems.getDefault().getPath(backupDir.toAbsolutePath().toString()+directorySeparator+"coremods");
	    Path configBackup = FileSystems.getDefault().getPath(backupDir.toAbsolutePath().toString()+directorySeparator+"config");

	    if (!Files.exists(modsBackup))
	    	modsBackup = Files.createDirectories(modsBackup);
	    if (!Files.exists(modsBackup)){
	    	System.out.println("Failed to create mods backup directory.");
	    	return false;
	    }
	    
/*	    if (!Files.exists(coremodsBackup))
	    	coremodsBackup = Files.createDirectories(coremodsBackup);
	    if (!Files.exists(coremodsBackup)){
	    	System.out.println("Failed to create coremods backup directory.");
	    	return false;
	    }*/
	    
	    if (!Files.exists(configBackup))
	    	configBackup = Files.createDirectories(configBackup);
	    if (!Files.exists(configBackup)){
	    	System.out.println("Failed to create config backup directory.");
	    	return false;
	    }
	    
	    System.out.println("Making backup of mods folder...");
		if (!isEmpty(mods, false)){
			if (!mergePaths(mods,modsBackup.toAbsolutePath().toString(), false)){
				System.out.println("Failed to backup mods.");
				return false;
			}
		} else
			System.out.println("Nothing to back up in mods.");
		
/*	    System.out.println("Making backup of coremods folder...");
		if (!isEmpty(coremods, false)){
			if (!mergePaths(coremods,coremodsBackup.toAbsolutePath().toString(), false)){
				System.out.println("Failed to backup coremods.");
				return false;
			}
		} else
			System.out.println("Nothing to back up in coremods.");*/
	    
	    System.out.println("Making backup of config folder...");
		if (!isEmpty(config, true)){
		    if (!mergePaths(config,configBackup.toAbsolutePath().toString(), true)){
			    System.out.println("Failed to backup config.");
			    return false;
			}
		} else
			System.out.println("Nothing to back up in config.");
	    	
	    return true;
	}
	
	public boolean moveToMC(String updateRoot, String mcRoot) throws IOException{
		Path updatePath = null;
		
		System.out.println("Updating mod files...");
		
		//Note: empty source directories will not work here
		
		//merge coremods folder (obsolete as of minecraft 1.6)
		//updatePath = FileSystems.getDefault().getPath(updateRoot+directorySeparator+"coremods");
		//if (!mergePaths(updatePath, mcRoot+directorySeparator+"coremods", true))
		//	return false;
		//merge mods folder
		updatePath = FileSystems.getDefault().getPath(updateRoot+directorySeparator+"mods");
		if (!mergePaths(updatePath, mcRoot+directorySeparator+"mods", true))
			return false;
		//merge config folder
		updatePath = FileSystems.getDefault().getPath(updateRoot+directorySeparator+"config");
		if (!mergePaths(updatePath, mcRoot+directorySeparator+"config", true))
			return false;
		
		return true;
	}
	
	// Returned .jar will replace the second jar and will use the first jar's metadata.
	public Path mergeJars(String jar1, String jar2) throws IOException{
		Path srcJar = FileSystems.getDefault().getPath(jar1);
		Path dstJar = FileSystems.getDefault().getPath(jar2);
		Path binBackup = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"backup"+directorySeparator+"bin");
		Path jar1Dir = null;
		Path jar2Dir = null;
		
	    if (!Files.exists(binBackup))
	    	binBackup = Files.createDirectories(binBackup);
	    if (!Files.exists(binBackup))
	    	throw new IOException("Failed to create bin backup directory.");
		
		if (Files.exists(srcJar))
			jar1Dir = extract(jar1);
		else
			throw new IOException("Missing Forge .jar");
		if (Files.exists(dstJar))
			jar2Dir = extract(jar2);
		else
			throw new IOException("Missing minecraft.jar, please re-run the minecraft launcher.");
		
		System.out.println("Merging "+jar1+" with "+jar2+"...");
		Path j2meta = FileSystems.getDefault().getPath(jar2Dir.toString()+ directorySeparator+"META-INF");
		deleteDir(j2meta);
		
		mergePaths(jar1Dir, jar2Dir.toString(), true);
		deleteDir(jar1Dir);
		dstJar = zip(jar2Dir.toFile(),dstJar.toFile());
		deleteDir(jar2Dir);
		
		return dstJar;
	}
	
	private boolean hasParentDir(Path p, String parentName){
		if (p.getParent() == null)
			return false;
		
		if (p.getFileName().toString().equals(parentName))
			return false;
		
		if (p.toAbsolutePath().toString().contains(parentName+directorySeparator))
			return true;
		
		return false;
	}
	
	private boolean mergePaths(Path source, String dest, boolean recursive) throws IOException{
		Path destPath = FileSystems.getDefault().getPath(dest);
		
		if (Files.isDirectory(source)){
			if (!Files.exists(destPath))
				Files.createDirectories(destPath);
			if (recursive){
				for (Path p : listDir(source)){
					if (Files.isDirectory(p)){
						mergePaths(p, destPath.toAbsolutePath().toString()+directorySeparator+p.getFileName().toString(), recursive);
						//Files.delete(source);
					}
					else 
						moveFile(p, destPath.toAbsolutePath().toString()+directorySeparator+p.getFileName().toString());
				}
			} else {
				for (Path p : listDir(source)){
					if (!Files.isDirectory(p))
						moveFile(p, destPath.toAbsolutePath().toString()+directorySeparator+p.getFileName().toString());
				}
			}
		} else
			return moveFile(source, destPath.toAbsolutePath().toString());
		
		return true;
	}
	
	private boolean moveFile(Path source, String dest) throws IOException{
		source = source.toAbsolutePath();
		Path destPath = FileSystems.getDefault().getPath(dest).toAbsolutePath();
		
		//do not move any old configs that do not match the new config names
		if (hasParentDir(source, "config")){
			Pattern relativeToMCDir = Pattern.compile("config[\\/, \\\\].+");
			Matcher matcher = relativeToMCDir.matcher(source.toAbsolutePath().toString());
			matcher.find();
			String relativePath = matcher.group(0);
			//Goddammit Windows
			if (SystemUtils.IS_OS_WINDOWS)
				relativePath = relativePath.replace('\\', '/');
			
			if (!newConfigs.contains(relativePath)){
				return true;
			}
		}
		
		Path res = Files.move(source, destPath, StandardCopyOption.REPLACE_EXISTING);
		if (verbose)
			System.out.println("Moved "+source.toAbsolutePath().toString()+" to "+destPath);
		return Files.exists(res);
	}
	
	public void moveForgeInstallers(Path dir) throws IOException{
		for (Path p : listDir(dir)){
			if (!Files.isDirectory(p) && p.getFileName().toString().matches(".+\\.jar")){
				Path newLocation = FileSystems.getDefault().getPath("ForgeInstaller.jar");
				System.out.println("Moving "+p.toString()+" to "+newLocation.toString());
				Files.copy(p, newLocation);
			}
		}
	}

	public void mergeInto(Path dir, String mcVersion, String instance) throws IOException, ZipException{
		if (Files.isDirectory(dir)){
			Path newJar = FileSystems.getDefault().getPath(mcFolder+directorySeparator+"versions"+directorySeparator+instance+directorySeparator+instance+".jar");
			Files.createDirectories(newJar.getParent());
			Files.copy(FileSystems.getDefault().getPath(mcFolder+directorySeparator+"versions"+directorySeparator+mcVersion+directorySeparator+mcVersion+".jar"), 
					newJar);
			
			for (Path p : listDir(dir)){
				if (!Files.isDirectory(p) && p.getFileName().toString().matches(".+\\.jar")){
					mergeJars(p.toString(), newJar.toString());
				}
				else if (!Files.isDirectory(p) && p.getFileName().toString().matches(".+\\.json")){
					Files.copy(p, FileSystems.getDefault().getPath(mcFolder+directorySeparator+"versions"+directorySeparator+instance+directorySeparator+instance+".json"));
				}
			}
		}
	}
	
	public ArrayList<Path> listDir(Path inPath){
		ArrayList<Path> files = new ArrayList<Path>();
		
		if (!Files.isDirectory(inPath)){
			files.add(inPath);
			return files;
		}
		
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(inPath)) {
			for (Path p : ds)
				files.add(p);
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return files;
	}
	
	public boolean isEmpty(Path dir, boolean recursive){
		if (Files.exists(dir)){
			if (Files.isDirectory(dir)){
				ArrayList<Path> files = listDir(dir);
				if (files.size() > 0){
					if (recursive)
						for (Path p : files){
							if (!isEmpty(p, recursive))
								return false;
						} 
					else
						for (Path p : files){
							if (!Files.isDirectory(p))
								return false;
						}
				} else
					return true;
			} else
				return false;
		}
		
		return true;
	}
	
	public void getZip(String zipUrl) throws IOException, NoSuchAlgorithmException{
		System.out.println("Downloading Client Mods zip...");
		URL zipURL = new URL(zipUrl);
		FileOutputStream fos = new FileOutputStream(this.folderName+".zip");
		long bytes = 0;
		
		try{
			ReadableByteChannel rbc = Channels.newChannel(zipURL.openStream());
			bytes = fos.getChannel().transferFrom(rbc, 0, 104857600);
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally{
			fos.close();
		}
		Path zipPath = FileSystems.getDefault().getPath(this.folderName+".zip");
		//return calculateMD5Hash(zipPath);
	}
	
	private String calculateMD5Hash(Path pth) throws IOException, NoSuchAlgorithmException{
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(Files.readAllBytes(pth));
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		String hashtext = bigInt.toString(16);
		// Now we need to zero pad it if you actually want the full 32 chars.
		while(hashtext.length() < 32 ){
		  hashtext = "0"+hashtext;
		}
		return hashtext;
	}
	
	public Path extract(String zipFile) throws IOException 
	{
		ZipFile zip = null;
		File tmpDir = null;
		String newPath = zipFile.substring(0, zipFile.length() - 4);
		try{
			
		    System.out.println("Extracting "+zipFile+"...");
		    int BUFFER = 4096;
		    File file = new File(zipFile);
	
		    zip = new ZipFile(file);
	
		    tmpDir = new File(newPath);
		    tmpDir.mkdir();
		    Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
	
		    // Process each entry
		    while (zipFileEntries.hasMoreElements())
		    {
		        // grab a zip file entry
		        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
		        String currentEntry = entry.getName();
		        File destFile = new File(newPath, currentEntry);
		        File destinationParent = destFile.getParentFile();
	
		        // create the parent directory structure if needed
		        destinationParent.mkdirs();
	
		        if (!entry.isDirectory()){
		            BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
		            int currentByte;
		            // establish buffer for writing file
		            byte data[] = new byte[BUFFER];
	
		            // write the current file to disk
		            FileOutputStream fos = new FileOutputStream(destFile);
		            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
	
		            // read and write until last byte is encountered
		            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
		                dest.write(data, 0, currentByte);
		            }
		            dest.flush();
		            dest.close();
		            is.close();
		        }
		        
		        if (!Files.isDirectory(destFile.toPath()) && currentEntry.contains("config/"))
		        	newConfigs.add(currentEntry);
		    }
		}
		catch(ZipException e){
			e.printStackTrace();
			return null;
		}
		finally{
			if (zip != null)
				zip.close();
		}
		return FileSystems.getDefault().getPath(newPath);
	}
	
	public Path zip(File directory, File zipfile) throws IOException {
		URI base = directory.toURI();
	    Deque<File> queue = new LinkedList<File>();
	    queue.push(directory);
	    OutputStream out = new FileOutputStream(zipfile);
	    Closeable res = out;
	    try {
	    	ZipOutputStream zout = new ZipOutputStream(out);
	    	res = zout;
	    	while (!queue.isEmpty()) {
	    		directory = queue.pop();
	    		for (File kid : directory.listFiles()) {
	    			String name = base.relativize(kid.toURI()).getPath();
	    			if (kid.isDirectory()) {
	    				queue.push(kid);
	    				name = name.endsWith("/") ? name : name + "/";
	    				zout.putNextEntry(new ZipEntry(name));
	    			} else {
	    				zout.putNextEntry(new ZipEntry(name));
	    				copy(kid, zout);
	    			}
	    		}
	    	}
	    } catch (Exception e){
	    	e.printStackTrace();
	    } finally {
	      res.close();
	    }
	    return zipfile.toPath();
	}
	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
		    if (readCount < 0) {
		    	break;
		    }
		    out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
	    }
	}
	
	private boolean deleteDir(Path dir) throws IOException{
		boolean ret = true;
		
		if (Files.isDirectory(dir)){
			for (Path p : listDir(dir)){	
				if (!deleteDir(p))
					ret = false;
			}
		}
		if (!Files.deleteIfExists(dir))
			ret = false;
		return ret;
	}
	
	public boolean cleanUp(boolean deleteZip) throws IOException{
		boolean ret = true;
		
		System.out.println("Removing temporary directory: "+this.folderName);
		if (!deleteDir(FileSystems.getDefault().getPath(this.folderName)))
			ret = false;
		
		if (deleteZip){
			System.out.println("Removing zipfile: "+this.folderName+".zip");
			if (!Files.deleteIfExists(FileSystems.getDefault().getPath(this.folderName+".zip")))
				ret = false;
		}
		return ret;
	}
}
