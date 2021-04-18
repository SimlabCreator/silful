package data.utility;

public class LogInData {

    //TODO: Update user, password and url
    public static String getUsername() {

        return "root";
    }

    public static String getPassword() {

        return "root";
    }

    public static String getUrl(){
        return "jdbc:mysql://localhost:3306/"+logic.utility.SettingsProvider.database+"?autoReconnect=true&useSSL=false&serverTimezone=UTC&useServerPrepStmts=false&rewriteBatchedStatements=true";
    }
}
