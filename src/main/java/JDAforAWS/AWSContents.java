package JDAforAWS;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

public class AWSContents {
	private AwsCredentials credentials;
	private Region region = Region.AP_NORTHEAST_1;
	
	public AWSContents() {
		this(new Property());
	}
	
	public AWSContents(Property property) {
		try {
			if (property.getProperty("AWS_Access_Key").equals("KEYHERE") || property.getProperty("AWS_Secret_Key").equals("KEYHERE")) {
				System.err.println("プロパティファイルのキーの値を変更してください。");
				System.exit(1);
			}
			credentials = AwsBasicCredentials.create(
					property.getProperty("AWS_Access_Key"),
					property.getProperty("AWS_Secret_Key")
					);
			System.out.println("AWS Done.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("AWS Error.");
		}
	}
	
	public AwsCredentials getCredentials() {
		return credentials;
	}
	
	public Region getRegion() {
		return region;
	}
	
	
}
