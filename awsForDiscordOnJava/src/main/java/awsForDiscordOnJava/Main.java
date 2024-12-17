package awsForDiscordOnJava;

import java.awt.Color;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

class Reload extends Thread {
	private EC2Contents aws;

	Reload(EC2Contents aws) {
		this.aws = aws;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			aws.reloadInstance();
		}
	}
}

public class Main {
	private static EC2Contents aws;
	private static JDA jda;

	public static void main(String[] args) throws InterruptedException {
		Property property = new Property();
		aws = new EC2Contents(property.getPropertyPath().toString());
		Scanner sc = new Scanner(System.in);
		try {
			jda = JDABuilder.createDefault(property.getProperty("BOT_TOKEN"))
					.setActivity(Activity.customStatus("コマンドを待機中"))
					.build().awaitReady();
			new SlashCommands(jda, Long.valueOf(property.getProperty("GUILD_ID")));
			eventListener();
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		property = null;
		new Reload(aws).start();
		System.out.println("起動完了！");
		System.out.println("exitと入力すると終了します。");
		switch (sc.next()) {
		case "exit": {
			aws = null;
			jda = null;
			sc.close();
			System.exit(1);
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
				default: {
					errorEmbed("指定されたコマンドが見つかりませんでした。", event);
				}

				}
			}

			@Override
			public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
				if (event.getName().equals("start") || event.getName().equals("stop")
						&& event.getFocusedOption().getName().equals("name"))
					;
				{
					Iterator<String> instances = aws.getInstanceName();
					List<Command.Choice> choices = new ArrayList<>();
					while (instances.hasNext()) {
						String instanceName = instances.next();
						if (instanceName.equals("discordbot")) {
							continue;
						}
						if (instanceName.toLowerCase()
								.startsWith(event.getFocusedOption().getValue().toLowerCase())) {
							choices.add(new Command.Choice(instanceName, instanceName));
							System.out.println("[AutoComplete]" + event.getUser() + ":" + instanceName);
						}
					}
					event.replyChoices(choices).queue();
				}
			}
		});
	}

	private static void start(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		String name = event.getOption("name").getAsString();
		System.out.println("start:" + name);
		String instanceState = aws.getAboutInstance(name, "State");
		EmbedBuilder embed = new EmbedBuilder();
		if (instanceState.equals("RUNNING")) {
			System.err.println("すでに起動しています。");
			errorEmbed("すでに起動しています。", event);
			return;
		}
		if(aws.startInstance(name)) {
			embed.setColor(Color.green)
			.setTitle("起動しました！")
			.setThumbnail("https://usasyuu.github.io/icon/power_symbol-1.png")
			.addField("Name", name, true)
			.addField("IPアドレス", aws.getAboutInstance(name, "PublicIpAddress"), true);
			System.out.println("Success Start:" + name);
		} else {
			System.err.println("起動に失敗しました。");
			errorEmbed("起動に失敗しました。", event);
			return;
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
		
	}

	private static void stop(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		String name = event.getOption("name").getAsString();
		System.out.println("stop:" + name);
		String instanceState = aws.getAboutInstance(name, "State");
		EmbedBuilder embed = new EmbedBuilder();
		if (instanceState.equals("STOPPED")) {
			System.err.println("すでに停止しています。");
			errorEmbed("すでに停止しています。", event);
			return;
		}
		if(aws.stopInstance(name)) {
			embed.setColor(Color.green)
			.setTitle("停止しました！")
			.setThumbnail("https://usasyuu.github.io/icon/power_symbolblack.png")
			.addField("Name", name, true);
			System.out.println("Success Stop:" + name);
		} else {
			System.err.println("停止に失敗しました。");
			errorEmbed("停止に失敗しました。", event);
			return;
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
	}

	private static void reload(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		aws.reloadInstance();
		successEmbed("サーバーリストを再読み込みしました！", event);
	}

	private static void getList(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		Iterator<String> list = aws.getInstanceName();
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle("サーバーリスト").setColor(Color.GREEN);

		while (list.hasNext()) {
			String instance = list.next();
			if (instance == "discordbot") {
				continue;
			}
			String instanceState = Main.aws.getAboutInstance(instance, "State");
			String instanceIp = Main.aws.getAboutInstance(instance, "PublicIpAddress");
			switch (instanceState) {
			case "RUNNING":
				instanceState = "オンライン";
				break;

			case "STOPPED":
				instanceState = "オフライン";
				break;

			default:
				instanceState = "待機中";
				break;
			}
			embed.addField(instance, instanceState + "\n" + instanceIp, true);
		}
		event.getHook().sendMessageEmbeds(embed.build()).queue();
	}

	private static EmbedBuilder successEmbed(String message, SlashCommandInteractionEvent event) {
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Color.green)
				.addField("Success", message, false);
		event.getHook().sendMessageEmbeds(embed.build()).queue();
		return embed;
	}

	private static EmbedBuilder errorEmbed(String message, SlashCommandInteractionEvent event) {
		EmbedBuilder embed = new EmbedBuilder()
				.setColor(Color.red)
				.addField("Error", message, false);
		event.getHook().sendMessageEmbeds(embed.build()).queue();
		return embed;
	}

	private static String getCommand(SlashCommandInteractionEvent event) {
		String commandName = event.getName();
		System.out.println(commandName + "Command" + "\s" + event.getUser());
		return commandName;

	}

}
