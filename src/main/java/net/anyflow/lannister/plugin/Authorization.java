package net.anyflow.lannister.plugin;

public interface Authorization extends Plugin {
	boolean isValid(String clientId);

	boolean isValid(boolean hasUserName, boolean hasPassword, String userName, String password);

	boolean isAuthorized(boolean hasUserName, String username);

}