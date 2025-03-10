package JDAforAWS;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import home.usami.mcsrvstatus.Mcsrvstatusapi;

class Reload extends Thread {
	private EC2Controller aws;

	Reload(EC2Controller aws) {
		this.aws = aws;
	}

	@Override
	public void run() {
		while (true) {
			try {
				sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			aws.reloadInstance();
		}
	}
}

public class Main {
	private static EC2Controller aws;
	private static JDA jda;
	private static final String version = "v1.3.0";

	public static void main(String[] args) {
		if (args.length != 0) {
			switch (args[0]) {
			case "version": {
				System.out.println(version);
				System.exit(0);
			}
			}
		}

		Scanner sc = new Scanner(System.in);
		System.out.print(getDateTime());
		Property property = new Property();
		System.out.print(getDateTime());
		aws = new EC2Controller(property);
		try {
			jda = JDABuilder.createDefault(property.getProperty("BOT_TOKEN"))
					.setActivity(Activity.customStatus("コマンドを待機中"))
					.setStatus(OnlineStatus.DO_NOT_DISTURB)
					.build().awaitReady();
			new SlashCommands(jda, Long.valueOf(property.getProperty("GUILD_ID")));
			eventListener();
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		property = null;
		new Reload(aws).start();
		System.out.print(getDateTime());
		System.out.println("起動完了！");
		jda.getPresence().setStatus(OnlineStatus.ONLINE);
		while (true) {
			System.out.print(getDateTime());
			System.out.println("exitで終了できます。");
			switch (sc.next()) {
			case "help": {
				System.out.println("\"exit\" = ボットを終了します。");
				break;
			}
			case "exit": {
				System.out.print(getDateTime());
				System.out.println("終了します。");
				sc.close();
				jda.shutdown();
				if (Files.exists(Paths.get("" + "InstanceList.json"))) {
					try {
						Files.delete(Paths.get("" + "InstanceList.json"));
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.exit(1);
			}
			}
		}

	}

	public static void eventListener() {
		jda.addEventListener(new ListenerAdapter() {
			@Override
			public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
				switch (getCommand(event)) {
				case "reload": {
					reload(event);
					break;
				}
				case "list": {
					getList(event);
					break;
				}
				case "start": {
					start(event);
					break;
				}
				case "stop": {
					stop(event);
					break;
				}
				case "status": {
					mcstatus(event);
					break;
				}
				default: {
					errorEmbed("指定されたコマンドが見つかりませんでした。", event);
				}
				}
				jda.getPresence().setStatus(OnlineStatus.ONLINE);
				jda.getPresence().setActivity(Activity.customStatus("コマンドを待機中"));
			}

			@Override
			public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
				if (event.getName().equals("start") 
						|| event.getName().equals("stop") 
						|| event.getName().equals("mcstatus")
						&& event.getFocusedOption().getName().equals("name"))
					;
				{
					Iterator<String> instances = aws.getInstanceName();
					List<Command.Choice> choices = new ArrayList<>();
					while (instances.hasNext()) {
						String instanceName = instances.next();
						if (instanceName.contains("ignore")) {
							continue;
						}
						if (instanceName.toLowerCase()
								.startsWith(event.getFocusedOption().getValue().toLowerCase())) {
							choices.add(new Command.Choice(instanceName, instanceName));
						}
					}
					event.replyChoices(choices).queue();
				}
			}
		});
	}

	private static void start(SlashCommandInteractionEvent event) {
		String name = event.getOption("name").getAsString();
		jda.getPresence().setActivity(Activity.customStatus(name + "を起動中"));
		System.out.print(getDateTime());
		System.out.println("start:" + name);
		String instanceState = aws.getAboutInstance(name, "State");
		EmbedBuilder embed = new EmbedBuilder();
		if(instanceState.equals("error")) {
			System.out.print(getDateTime());
			System.err.println("存在しません。");
			errorEmbed("存在しないインスタンスです。", event);
			return;
		}
		if (instanceState.equals("RUNNING")) {
			System.out.print(getDateTime());
			System.err.println("すでに起動しています。");
			errorEmbed("すでに起動しています。", event);
			return;
		}
		if (aws.startInstance(name)) {
			embed.setColor(Color.green)
					.setTitle("起動しました！")
					.setThumbnail("https://usasyuu.github.io/icon/power_symbol-1.png")
					.addField("Name", name, true)
					.addField("IPアドレス", aws.getAboutInstance(name, "PublicIpAddress"), true);
			System.out.print(getDateTime());
			System.out.println("Success Start:" + name);
		} else {
			System.out.print(getDateTime());
			System.err.println("起動に失敗しました。");
			errorEmbed("起動に失敗しました。", event);
			return;
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();

	}

	private static void stop(SlashCommandInteractionEvent event) {
		String name = event.getOption("name").getAsString();
		jda.getPresence().setActivity(Activity.customStatus(name + "を停止中"));
		System.out.print(getDateTime());
		System.out.println("stop:" + name);
		String instanceState = aws.getAboutInstance(name, "State");
		EmbedBuilder embed = new EmbedBuilder();
		if(instanceState.equals("error")) {
			System.out.print(getDateTime());
			System.err.println("存在しません。");
			errorEmbed("存在しないインスタンスです。", event);
			return;
		}
		if (instanceState.equals("STOPPED")) {
			System.out.print(getDateTime());
			System.err.println("すでに停止しています。");
			errorEmbed("すでに停止しています。", event);
			return;
		}
		if (aws.stopInstance(name)) {
			embed.setColor(Color.green)
					.setTitle("停止しました！")
					.setThumbnail("https://usasyuu.github.io/icon/power_symbolblack.png")
					.addField("Name", name, true);
			System.out.print(getDateTime());
			System.out.println("Success Stop:" + name);
		} else {
			System.out.print(getDateTime());
			System.err.println("停止に失敗しました。");
			errorEmbed("停止に失敗しました。", event);
			return;
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
	}

	private static void reload(SlashCommandInteractionEvent event) {
		aws.reloadInstance();
		successEmbed("サーバーリストを再読み込みしました！", event);
	}

	private static void mcstatus(SlashCommandInteractionEvent event) {
		String name = event.getOption("name").getAsString();
		jda.getPresence().setActivity(Activity.customStatus(name + "を確認中"));
		System.out.print(getDateTime());
		System.out.println("mcstatus:" + name);
		Mcsrvstatusapi mcsrv_api = new Mcsrvstatusapi(aws.getAboutInstance(name, "PublicIpAddress"));
		EmbedBuilder embed = new EmbedBuilder();
		boolean beOnline = mcsrv_api.beOnline();
		String beOnline_s;
		embed = embed.setTitle(name)
			.addField("IPアドレス", aws.getAboutInstance(name, "PublicIpAddress"), true)
			.setThumbnail("https://usasyuu.github.io/icon/Minecraft_Downloads.png");
		if (!beOnline) {
			beOnline_s = "オフライン";
			embed.setColor(Color.RED);
		} else {
			beOnline_s = "オンライン";
			embed.setColor(Color.GREEN).setDescription(mcsrv_api.getMOTDclean())
			.addField("プレイヤー数", String.valueOf(mcsrv_api.getOnlinePlayerNum()) + "/" + String.valueOf(mcsrv_api.getMaxPlayerNum()) , true)
			.addField("バージョン", mcsrv_api.getMCVersion(), true);
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
	}

	private static void getList(SlashCommandInteractionEvent event) {
		Iterator<String> list = aws.getInstanceName();
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle("サーバーリスト").setColor(Color.GREEN);

		while (list.hasNext()) {
			String instance = list.next();
			if (instance.contains("ignore")) {
				continue;
			}
			String instanceState = aws.getAboutInstance(instance, "State");
			String instanceIp = aws.getAboutInstance(instance, "PublicIpAddress");
			switch (instanceState) {
			case "RUNNING" ->
				instanceState = "オンライン";

			case "STOPPED" ->
				instanceState = "オフライン";

			default ->
				instanceState = "待機中";
			}
			embed.addField(instance, instanceState + "\n" + instanceIp, true);
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
	}

	private static EmbedBuilder successEmbed(String message, SlashCommandInteractionEvent event) {
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Color.green)
				.setTitle("Success")
				.setDescription(message);
		event.getHook().sendMessageEmbeds(embed.build()).queue();
		return embed;
	}

	private static EmbedBuilder errorEmbed(String message, SlashCommandInteractionEvent event) {
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Color.red)
				.setTitle("Error")
				.setDescription(message);
		event.getHook().sendMessageEmbeds(embed.build()).queue();
		return embed;
	}

	private static String getCommand(SlashCommandInteractionEvent event) {
		jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
		event.deferReply().queue();
		String commandName = event.getName();
		System.out.print(getDateTime());
		System.out.println(commandName + "Command" + "\s" + event.getUser());
		return commandName;
	}

	private static String getDateTime() {
		Calendar clr = Calendar.getInstance();

		String year = String.valueOf(clr.get(Calendar.YEAR));
		String month = addZero(clr.get(Calendar.MONTH) + 1);
		String date = addZero(clr.get(Calendar.DATE));
		String hour = addZero(clr.get(Calendar.HOUR));
		String minute = addZero(clr.get(Calendar.MINUTE));
		String second = addZero(clr.get(Calendar.SECOND));

		return year + "-" + month + "-" + date + " " + hour + ":" + minute + ":" + second + " ";
	}

	private static String addZero(int t) {
		return t < 10 ? "0" + t : t + "";
	}
}
