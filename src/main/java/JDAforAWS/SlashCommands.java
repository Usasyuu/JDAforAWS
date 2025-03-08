package JDAforAWS;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;

public class SlashCommands {
//	private long guildId;
	public SlashCommands(JDA jda, long guildId) {
//		this.guildId = guildId;
		registCommands(jda);
	}
	
	public void registCommands(JDA jda) {
		jda.updateCommands().addCommands(
				Commands.slash("reload", "サーバーリストを更新します。")
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
				
				Commands.slash("list", "サーバーリストを表示します。"),
				
				Commands.slash("start", "サーバーを起動します。")
				.addOption(OptionType.STRING, "name", "サーバーの名前を入力してください。", true, true),
				
				Commands.slash("stop", "サーバーを停止します。")
				.addOption(OptionType.STRING, "name", "サーバーの名前を入力してください。", true, true)
				
				).queue();
	}
	
}
