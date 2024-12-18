package awsForDiscordOnJava;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Properties;

public class Property {
	private static Path path;
	private static Path folderPath;
	private Properties properties = new Properties();
	private FileInputStream istream;

	public Property() {
		Boolean os = File.separatorChar == '\\';
		folderPath = Paths.get("").toAbsolutePath();
		if(os) {
			path = Paths.get(folderPath + "\\" + "settings.properties");
		} else {
			path = Paths.get(folderPath + "/" + "settings.properties");
		}
		
		if(!Files.exists(path)) {
			System.out.println("Make Properties File.");
			makeProperties();
			System.out.println("必要な情報をプロパティファイルに書き込んでください。");
			Scanner sc = new Scanner(System.in);
			sc.nextLine();
			sc.close();
			System.exit(1);
		} else {
			System.out.println("Already exist.");
			try {
				istream = new FileInputStream(path.toString());
			} catch (FileNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		
	}
	public String getProperty(String property) {
		try {
			properties.load(istream);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return properties.getProperty(property);
	}
	
	public Path getPropertyPath() {
		return path;
	}
	
	public static Path getPath() {
		return folderPath;
	}

	private static void makeProperties() {
		try {
			Files.createFile(path);
		} catch (IOException e) {
			System.out.println(e);
			return;
		}
		Properties properties = new Properties();
		//ここを変更↓
		properties.setProperty("AWS_Access_Key", "xxxxxxxxxxxxx");
        properties.setProperty("AWS_Secret_Key", "xxxxxxxxxxxxx");
		properties.setProperty("BOT_TOKEN", "xxxxxxxxxxxxx");
		properties.setProperty("GUILD_ID", "xxxxxxxxxxxxx");
        //ここを変更↑
        try {
			properties.store(new FileOutputStream(path.toString()), "Comments");
		} catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return;
		}
	}
	
}
