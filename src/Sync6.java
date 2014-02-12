import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.SystemUtils;


public class Sync6 {

	//private String zipLocation = "https://dl.dropbox.com/u/20588547/Buddhacraft/Client/BuddhacraftClient.zip";
	private String zipLocation = "C:\\tmp\\BuddhacraftClient.zip";
	private String folderName = "BuddhacraftClient";
	
	public Sync6(String zipURL, String tempFolder){
		this.zipLocation = zipURL;
		this.folderName = tempFolder;
	}
	
	public boolean moveFiles(){
		String mcFolder = "";
		File updateDir = new File(this.folderName);
		
		if (SystemUtils.IS_OS_WINDOWS){
			mcFolder = System.getenv("APPDATA")+"\\.minecraft";
		}
		else if (SystemUtils.IS_OS_MAC_OSX){
			mcFolder = "/users/library/application support/minecraft";
		}
		else{
			mcFolder = System.getProperty("user.home")+"/.minecraft";
		}
		
		File mods = new File(mcFolder+"/mods");
		File coremods = new File(mcFolder+"/coremods");
		File config = new File(mcFolder+"/config");		
		File backupDir = new File(mcFolder+"/backup");	
		if (!makeBackup(backupDir, mods, coremods, config))
			return false;
		
		for (File f : updateDir.listFiles()){
			File nf = new File(mcFolder+"/"+f.getName());
			boolean res = f.renameTo(nf);
			System.out.println("Moved "+f.getAbsolutePath()+" to "+nf.getAbsolutePath()+": "+res);
			if (!res){
				return false;
			}
		}
		return true;
	}
	public boolean makeBackup(File backupDir, File mods, File coremods, File config){
	    File modsBackup = new File(backupDir.getAbsolutePath()+"/mods");
	    File coremodsBackup = new File(backupDir.getAbsolutePath()+"/coremods");
	    File configBackup = new File(backupDir.getAbsolutePath()+"/config");
	    boolean backedUp = false;
	    
	    backedUp = backupDir.exists();
	    if (!backedUp)
	    	backedUp = backupDir.mkdir();
	    if (!backedUp){
	    	System.out.println("Failed to create backup directory.");
	    	return false;
	    }
	    
	    backedUp = modsBackup.exists();
	    if (!backedUp)
	    	backedUp = modsBackup.mkdir();
	    if (!backedUp){
	    	System.out.println("Failed to create mods backup directory.");
	    	return false;
	    }
	    
	    backedUp = coremodsBackup.exists();
	    if (!backedUp)
	    	backedUp = coremodsBackup.mkdir();
	    if (!backedUp){
	    	System.out.println("Failed to create coremods backup directory.");
	    	return false;
	    }
	    
	    backedUp = configBackup.exists();
	    if (!backedUp)
	    	backedUp = configBackup.mkdir();
	    if (!backedUp){
	    	System.out.println("Failed to create config backup directory.");
	    	return false;
	    }
	    
	    System.out.println("Making backup of mods folder...");
		if (needsBackup(mods, false))
		    for (File f : mods.listFiles()){
				if (!f.isDirectory()){
			    	File nf = new File(modsBackup.getAbsolutePath()+"/"+f.getName());
			    	System.out.println("Moving "+f.getAbsolutePath()+" to "+nf.getAbsolutePath());
			    	boolean res = f.renameTo(nf);
			    	if (!res){
			    		System.out.println(nf.getName()+" failed to move to backup directory.");
			    		System.out.println("Failed to backup mods.");
			    		return false;
			    	}
			    }
			}
		else
			System.out.println("Nothing to back up in mods.");
		
	    System.out.println("Making backup of coremods folder...");
	    if (needsBackup(coremods, false))
			for (File f : coremods.listFiles()){
				if (!f.isDirectory()){
			    	File nf = new File(coremodsBackup.getAbsolutePath()+"/"+f.getName());
			    	System.out.println("Moving "+f.getAbsolutePath()+" to "+nf.getAbsolutePath());
			    	boolean res = f.renameTo(nf);
			    	if (!res){
			    		System.out.println(nf.getName()+" failed to move to backup directory.");
			    		System.out.println("Failed to backup coremods.");
			    		return false;
			    	}
			    }
			}
	    else
	    	System.out.println("Nothing to back up in coremods.");
	    
	    System.out.println("Making backup of config folder...");
	    if (needsBackup(config, true))
			for (File f : config.listFiles()){
			    File nf = new File(configBackup.getAbsolutePath()+"/"+f.getName());
		    	System.out.println("Moving "+f.getAbsolutePath()+" to "+nf.getAbsolutePath());
		    	boolean res = f.renameTo(nf);
		    	if (!res){
		    		System.out.println(nf.getName()+" failed to move to backup directory.");
		    		System.out.println("Failed to backup configs.");
		    		return false;
		    	}
			}
	    else
	    	System.out.println("Nothing to back up in config.");
	    	
	    return true;
	}
	
	public boolean needsBackup(File dir, boolean recursive){
		boolean ret = false;
		File[] files = dir.listFiles();
		
		if (dir.exists()){
			if (dir.isDirectory()){
				if (files.length > 0){
					if (recursive)
						for (File f : files){
							if (needsBackup(f, recursive))
								ret = true;
						}
					else
						for (File f : files){
							if (!f.isDirectory())
								ret = true;
						}
				}
				else
					ret = false;
					
			}
			else
				ret = true;
		}
		return ret;
	}
	
	public long getZip() throws IOException{
		System.out.println("Downloading Client Mods zip...");
		URL zipURL = new URL(this.zipLocation);
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
		return bytes;
	}
	
	public boolean extract() throws IOException 
	{
		ZipFile zip = null;
		try{
			String zipFile = this.folderName+".zip";
		    System.out.println("Extracting "+zipFile+"...");
		    int BUFFER = 2048;
		    File file = new File(zipFile);
		    file.deleteOnExit();
	
		    zip = new ZipFile(file);
		    String newPath = zipFile.substring(0, zipFile.length() - 4);
	
		    File tmpDir = new File(newPath);
		    tmpDir.mkdir();
		    tmpDir.deleteOnExit();
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
	
		        if (!entry.isDirectory())
		        {
		            BufferedInputStream is = new BufferedInputStream(zip
		            .getInputStream(entry));
		            int currentByte;
		            // establish buffer for writing file
		            byte data[] = new byte[BUFFER];
	
		            // write the current file to disk
		            FileOutputStream fos = new FileOutputStream(destFile);
		            BufferedOutputStream dest = new BufferedOutputStream(fos,
		            BUFFER);
	
		            // read and write until last byte is encountered
		            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
		                dest.write(data, 0, currentByte);
		            }
		            dest.flush();
		            dest.close();
		            is.close();
		        }
		    }
			}
		catch(ZipException e){
			e.printStackTrace();
			return false;
		}
		finally{
			if (zip != null)
				zip.close();
		}
		return true;
	}

}
