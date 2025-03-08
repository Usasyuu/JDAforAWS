package JDAforAWS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Property {
	private Properties properties = new Properties();
	private Path path = Paths.get("" + "settings.properties");
	
	public Property() {
		if (Files.exists(path)) {
			System.out.println("Properties File is exist.");
			System.out.println(path.toAbsolutePath().toString());
			loadProperty();
		} else {
			System.err.println("Properties File is not exist.");
			makeProperty();
			System.exit(1);
		}
	}

	private void setProperty() {
//		properties.setProperty("Key", "Value");
		properties.setProperty("AWS_Access_Key", "KEYHERE");
		properties.setProperty("AWS_Secret_Key", "KEYHERE");
		properties.setProperty("BOT_TOKEN", "KEYHERE");
		properties.setProperty("GUILD_ID", "KEYHERE");
	}
	
	private void makeProperty() {
		System.out.println("Making Property File.");
		try {
			Files.createFile(path);
			setProperty();
			properties.store(new FileOutputStream(path.toString()), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadProperty() {
		try {
			properties.load(new FileInputStream(path.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getProperty(String property) {
		return properties.getProperty(property);
	}
}
