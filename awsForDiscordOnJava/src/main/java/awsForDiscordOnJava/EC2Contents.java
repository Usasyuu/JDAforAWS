package awsForDiscordOnJava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EC2Contents extends AWSContents {
	private Ec2Client ec2;
	private ObjectMapper mapper = new ObjectMapper();
	private final String jsonFile = Property.getPath().toString() + "\\instanceList.json";
	
	public EC2Contents(String path){
		super(path);
		try {
			System.out.println("EC2 Startup...");
			ec2 = Ec2Client.builder()
				.region(Region.AP_NORTHEAST_1)
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();
			writeAllInstance();
		} catch (Ec2Exception e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
			System.exit(0);
		} catch (ExceptionInInitializerError e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
			System.exit(0);
		} catch (StreamWriteException e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
			System.exit(0);
		} catch (DatabindException e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
			System.exit(0);
		} catch (IOException e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
			System.exit(0);
		}
	}
	public boolean startInstance(String instanceName) {
		try {
			String instanceId = getAboutInstance(instanceName, "InstanceId");
			if (instanceId == null) {
				return false;
			}
			StartInstancesRequest request = StartInstancesRequest.builder()
					.instanceIds(instanceId)
					.build();
			ec2.startInstances(request);
			Ec2Waiter waiter = ec2.waiter();
			waiter.waitUntilInstanceRunning(DescribeInstancesRequest.builder()
					.instanceIds(instanceId)
					.build());
			return true;
		} catch (NullPointerException e) {
			System.err.println("インスタンス検索中にエラーが発生しました: " + e.getMessage());
		} catch (Ec2Exception e) {
			System.err.println("EC2開始中にエラーが発生しました: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
		}
		return false;
	}
	public boolean stopInstance(String instanceName) {
		try {
			String instanceId = getAboutInstance(instanceName, "InstanceId");
			if (instanceId == null) {
				return false;
			}
			StopInstancesRequest request = StopInstancesRequest.builder()
	                .instanceIds(instanceId)
	                .build();
			ec2.stopInstances(request);
			Ec2Waiter waiter = ec2.waiter();
			waiter.waitUntilInstanceStopped(DescribeInstancesRequest.builder()
			        .instanceIds(instanceId)
			        .build());
			return true;
		} catch (NullPointerException e) {
			System.err.println("インスタンス検索中にエラーが発生しました: " + e.getMessage());
		} catch (Ec2Exception e) {
			System.err.println("EC2停止中にエラーが発生しました: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("予期しないエラーが発生しました: " + e.getMessage());
		}
		return false;
	}
	
	public String getAboutInstance(String instanceName, String type) {
		JsonNode root;
		if(instanceName.equals("DiscordBot")) {
			return null;
		}
		try {
			root = mapper.readTree(new File(jsonFile));
			String instanceInfo = root.get("instances").get(instanceName).get(0).get(type).asText();
			return instanceInfo;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	public Iterator<String> getInstanceName() {
		try {
			JsonNode root = mapper.readTree(new File(jsonFile));
			JsonNode instances = root.get("instances");
			Iterator<String> fieldNames = instances.fieldNames();
			return fieldNames;
			
			
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return null;
		}
	}
	
	public void reloadInstance() {
		try {
			writeAllInstance();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
	private void writeAllInstance() throws StreamWriteException, DatabindException, IOException {
		DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
		DescribeInstancesResponse response = ec2.describeInstances(describeInstancesRequest);
		Map<String, Map<String, List<Map<String, Object>>>> instancesByTagAndValue = new HashMap<>();
		
		response.reservations().stream()
        .flatMap(reservation -> reservation.instances().stream())
        .forEach(instance -> {
            instance.tags().forEach(tag -> {
                instancesByTagAndValue.computeIfAbsent("instances", k -> new HashMap<>())
                        .computeIfAbsent(tag.value(), v -> new ArrayList<>())
                        .add(createInstanceInfoMap(instance));
            });
        });

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), instancesByTagAndValue);
	}
	
    private static Map<String, Object> createInstanceInfoMap(Instance instance) {
        Map<String, Object> instanceInfo = new HashMap<>();
        instanceInfo.put("PublicIpAddress", instance.publicIpAddress());
        instanceInfo.put("InstanceId", instance.instanceId());
        instanceInfo.put("State", instance.state().name());

        return instanceInfo;
    }
    
}
