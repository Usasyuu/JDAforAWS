package awsForDiscordOnJava;

import java.util.Properties;
import java.io.FileInputStream;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

public class AWSContents {
	protected AwsCredentials credentials;
	protected Region region = Region.AP_NORTHEAST_1;
	
	public AWSContents(String path) {
		try {
			Properties properties = new Properties();
			FileInputStream istream = new FileInputStream(path);
			properties.load(istream);
			try {
				credentials = AwsBasicCredentials.create(
						 properties.getProperty("AWS_Access_Key"),
						 properties.getProperty("AWS_Secret_Key")
						 );
				System.out.println("AWS Done.");
			} catch (Exception e) {
				System.out.println("AWS Error.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
