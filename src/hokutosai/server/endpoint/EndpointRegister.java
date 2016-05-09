package hokutosai.server.endpoint;

import java.io.*;

public class EndpointRegister {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) throw new Exception("入力ファイル名と出力ファイル名を指定してください．");
		String inputFilePath = "endpoint/HokutosaiApi2016.ep";
		String outputFilePath = "/Users/Shuka/Documents/高専/北斗祭2016/Develop/HokutosaiApi2016.sql";

		BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFilePath)));

		try {
			EndpointRegister er = new EndpointRegister();
			er.run(br, pw);
			System.out.println("[SUCCESS] Saved sql file to " + outputFilePath);
		} catch (Exception e) {
			System.out.println("[ERROR] " + e.getMessage());
			pw.println("###### ERROR ######");
		} finally {
			br.close();
			pw.close();
		}
	}

	public EndpointRegister() {

	}

	public void run(BufferedReader br, PrintWriter pw) throws Exception {
		pw.println("DELETE FROM `endpoints_accounts_permissions`;");
		pw.println("DELETE FROM `endpoints_api_users_permissions`;");
		pw.println("DELETE FROM `api_endpoints`;");
		pw.println("DELETE FROM `api_categories`;");

		pw.println("TRUNCATE TABLE `endpoints_api_users_permissions`;");
		pw.println("TRUNCATE TABLE `endpoints_accounts_permissions`;");
		pw.println();

		String line = br.readLine();
		while (line != null) {
			if (!line.isEmpty()) {
				this.processLine(line, pw);
			}
			line = br.readLine();
		}
	}

	private void processLine(String line, PrintWriter pw) throws Exception {
		switch (line.charAt(0)) {
		case '#':
			return;
		case '@':
			this.registCategory(line.substring(1), pw);
			break;
		default:
			this.registEndpoint(line, pw);
			break;
		}
	}

	private String currentCategory = null;

	private void registCategory(String category, PrintWriter pw) {
		if (this.currentCategory != null) pw.println();

		this.currentCategory = category;

		pw.printf("--\n-- @%s\n--\n", this.currentCategory);
		pw.printf("INSERT INTO `api_categories` (`category`, `permission`) VALUES (\'%s\', \'allow\');\n", this.currentCategory);
		System.out.printf("@%s\n", this.currentCategory);
	}

	private void registEndpoint(String line, PrintWriter pw) throws Exception {
		String[] splited = line.split("[\\s]+");
		if (splited.length < 3) throw new Exception("Bad format.");

		String method = splited[0].toUpperCase();
		String path = splited[1];
		System.out.printf("%s %s\n", method, path);

		String apiUserPermission = splited[2];
		String accountPermission = splited.length > 3 ? splited[3] : null;
		String accountNeed = this.getAccountNeed(accountPermission);

		pw.printf("-- %s %s\n", method, path);
		pw.printf("INSERT INTO api_endpoints (`path`, `method`, `category`, `account_need`) VALUES (\'%s\', \'%s\', \'%s\', \'%s\');\n", path, method, this.currentCategory, accountNeed);
		this.registEndpointApiUserPermission(apiUserPermission, path, method, pw);

		if (accountPermission != null) {
			this.registEndpointAccountPermission(accountPermission.substring(1), path, method, pw);
		}
	}

	private String getAccountNeed(String accountPermission) throws Exception {
		if (accountPermission != null) {
			switch (accountPermission.charAt(0)) {
			case '-':
				return "optional";
			case '+':
				return "required";
			default:
				throw new Exception("Bad format. (AccountPermission)");
			}
		}

		return "no";
	}

	private static final String[] apiUsers = {"admin", "client", "management"};

	private void registEndpointApiUserPermission(String apiUserPermission, String path, String method, PrintWriter pw) throws Exception {
		String[] permissions = {"allow", "deny", "deny"};

		int length = apiUserPermission.length();
		for (int i = 0; i < length; ++i) {
			switch (apiUserPermission.charAt(i)) {
			case 'a':
				permissions[0] = "allow";
				break;
			case 'c':
				permissions[1] = "allow";
				break;
			case 'm':
				permissions[2] = "allow";
				break;
			default:
				throw new Exception("ApiUserRoleが存在しません．");
			}
		}

		for (int i = 0; i < apiUsers.length; ++i) {
			pw.printf("INSERT INTO endpoints_api_users_permissions (`path`, `method`, `api_user_role`, `permission`) VALUES (\'%s\', \'%s\', \'%s\', \'%s\');\n", path, method, apiUsers[i], permissions[i]);
		}
	}

	private static final String[] accounts = {"root", "general", "management"};

	private void registEndpointAccountPermission(String accountPermission, String path, String method, PrintWriter pw) throws Exception {
		String[] permissions = {"allow", "deny", "deny"};

		int length = accountPermission.length();
		for (int i = 0; i < length; ++i) {
			switch (accountPermission.charAt(i)) {
			case 'r':
				permissions[0] = "allow";
				break;
			case 'g':
				permissions[1] = "allow";
				break;
			case 'm':
				permissions[2] = "allow";
				break;
			default:
				throw new Exception("AccountRoleが存在しません．");
			}
		}

		for (int i = 0; i < accounts.length; ++i) {
			pw.printf("INSERT INTO endpoints_accounts_permissions (`path`, `method`, `account_role`, `permission`) VALUES (\'%s\', \'%s\', \'%s\', \'%s\');\n", path, method, accounts[i], permissions[i]);
		}
	}

}
