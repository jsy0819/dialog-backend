package com.dialog.security.oauth2;

import java.util.Map;

public class GoogleUserInfo implements SocialUserInfo {

	private final Map<String, Object> attributes;
		
	public GoogleUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub//
		return (String) attributes.get("sub");
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return (String) attributes.get("name");
	}

	@Override
	public String getEmail() {
		// TODO Auto-generated method stub
		return (String) attributes.get("email");
	}

	@Override
	public String getProfileImageUrl() {
		// TODO Auto-generated method stub
		return (String) attributes.get("picture");
	}

}

