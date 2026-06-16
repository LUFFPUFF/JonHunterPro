package ru.jobhunter.infrastructure.platform.oauth;

public interface OAuthCustomSchemeCallbackHandler {

    boolean supports(String callbackUri);

    boolean dispatch(String callbackUri);
}
