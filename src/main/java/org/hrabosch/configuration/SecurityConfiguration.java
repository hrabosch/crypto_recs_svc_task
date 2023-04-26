package org.hrabosch.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${whitelistedExpression}")
    private String whitelistedExpression;

    @Bean
    public SecurityFilterChain filterChainWhitelistedIPs(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests()
                .requestMatchers(toH2Console()).permitAll()
                .and()
                .csrf().ignoringRequestMatchers(toH2Console())
                .and()
                .authorizeHttpRequests()
                .anyRequest()
                .access(new WebExpressionAuthorizationManager(whitelistedExpression));

        http.headers().frameOptions().disable();
        return http.build();
    }

    @Bean
    @Profile("h2")
    public SecurityFilterChain filterChainH2Console(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .requestMatchers(toH2Console()).permitAll()
                .and()
                .csrf().ignoringRequestMatchers(toH2Console());

        http.headers().frameOptions().disable();
        return http.build();
    }

}
