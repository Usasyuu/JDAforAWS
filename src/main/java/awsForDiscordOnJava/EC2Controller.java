package awsForDiscordOnJava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

public class EC2Controller extends AWSContents {
	private Ec2Client ec2;
	private ObjectMapper mapper = new ObjectMapper();
	private Path jsonPath = Paths.get("" + "InstanceList.json");
	private File jsonFile = new File(jsonPath.toString());
	
	public EC2Controller(Property property){
		super(property);
		try {
			ec2 = Ec2Client.builder()
				.region(super.getRegion())
				.credentialsProvider(StaticCredentialsProvider.create(super.getCredentials()))
				.build();
			if (Files.exists(jsonPath)) {
				System.out.println("InstanceList.json is exist");
			} else {
				Files.createFile(jsonPath);
			}
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
		try {
			root = mapper.readTree(jsonFile);
			String instanceId = root.get("instances").get(instanceName).get(0).get(type).asText();
			return instanceId;
		} catch (IOException e) {
			return "見つかりませんでした。";
		}
	}
	
	public Iterator<String> getInstanceName() {
		try {
			JsonNode root = mapper.readTree(jsonFile);
			JsonNode instances = root.get("instances");
			Iterator<String> fieldNames = instances.fieldNames();
			return fieldNames;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void reloadInstance() {
		try {
			writeAllInstance();
		} catch (IOException e) {
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

        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, instancesByTagAndValue);
	}
	
    private static Map<String, Object> createInstanceInfoMap(Instance instance) {
        Map<String, Object> instanceInfo = new HashMap<>();
        instanceInfo.put("PublicIpAddress", instance.publicIpAddress());
        instanceInfo.put("InstanceId", instance.instanceId());
        instanceInfo.put("State", instance.state().name());

        return instanceInfo;
    }
    
}
