import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;


public class Syncer {

	public static void main(String[] args) throws ParseException {

		
		Options options = new Options();
		//options.addOption("j", true, "Merge jarmods into the specified version of minecraft.");
		options.addOption("f", false, "Run forge installers.");
		options.addOption("v", false, "Verbose output.");
		options.addOption("d", false, "Delete client mods zip when finished.");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		try{
			Yaml yaml = new Yaml();
			InputStream yamlInStream = Syncer.class.getClassLoader().getResourceAsStream("config.yaml");

			//For running within eclipse, debugging purposes only
			//InputStream yamlInStream = new FileInputStream(new File("config.yaml"));
			
			if (yamlInStream == null)
				throw new IOException("Failed to open config.yaml");
			@SuppressWarnings("unchecked")
			HashMap<String, String> strings = (HashMap<String, String>) yaml.load(yamlInStream);
			String zipUrl = strings.get("zipUrl");
			String tmpDir = strings.get("tmpDir");
			String instance = strings.get("instanceName");
			yamlInStream.close();
			Path updateDir = FileSystems.getDefault().getPath(tmpDir);
			Sync7 s = new Sync7(tmpDir);
			
			if (cmd.hasOption('v'))
				s.enableVerbose();
			// Download the zip
			s.getZip(zipUrl);
			// extract the zip and update minecraft files to match its contents
			s.extract(tmpDir+".zip");
			s.moveToBackup(s.getMcFolder());
			s.moveToMC(updateDir.toAbsolutePath().toString(), s.getMcFolder());
			// Install jarmods if -j is set
			//if (cmd.hasOption('j'))
			//	s.mergeInto(FileSystems.getDefault().getPath(tmpDir+s.getDirectorySeparator()+"jarmods"), cmd.getOptionValue("j"), instance);
			if (cmd.hasOption('f'))
				s.moveForgeInstallers(FileSystems.getDefault().getPath(tmpDir+s.getDirectorySeparator()+"forge"));
			s.cleanUp(cmd.hasOption("d"));
			System.out.println("Successfully updated minecraft client!");
		}
		catch (Exception e){
			System.out.println("Failed to update minecraft client.");
			e.printStackTrace();
			//System.out.println(e.getMessage());
		}
	}

}
