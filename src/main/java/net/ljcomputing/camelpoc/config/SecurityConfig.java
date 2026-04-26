package net.ljcomputing.camelpoc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import net.ljcomputing.camelpoc.filter.JwtAuthenticationFilter;
import net.ljcomputing.camelpoc.security.JwtTokenService;
import net.ljcomputing.camelpoc.security.UnauthorizedEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.username}")
    private String ldapManagerDn;

    @Value("${spring.ldap.password}")
    private String ldapManagerPassword;

    @Bean
    @Primary
    SecurityFilterChain filterChain(
        final HttpSecurity http, final JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/oauth/token", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                .authenticationProvider(ldapAuthenticationProvider())
                // .httpBasic(Customizer.withDefaults())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                );
        return http.build();
    }

    @Bean
    DefaultSpringSecurityContextSource contextSource() {
        final DefaultSpringSecurityContextSource source = new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);
        source.setUserDn(ldapManagerDn);
        source.setPassword(ldapManagerPassword);
        return source;
    }

    @Bean
    BindAuthenticator bindAuthenticator() {
        final BindAuthenticator authenticator = new BindAuthenticator(contextSource());
        authenticator.setUserDnPatterns(new String[] { "uid={0},ou=users" });
        return authenticator;
    }

    @Bean
    DefaultLdapAuthoritiesPopulator authoritiesPopulator() {
        final DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource(), "ou=groups");
        populator.setGroupSearchFilter("(member={0})");
        populator.setGroupRoleAttribute("cn");
        return populator;
    }

    @Bean
    LdapAuthenticationProvider ldapAuthenticationProvider() {
        return new LdapAuthenticationProvider(bindAuthenticator(), authoritiesPopulator());
    }

    @Bean
    AuthenticationManager authenticationManager() {
        return new ProviderManager(ldapAuthenticationProvider());
    }

    @Bean
    UnauthorizedEntryPoint unauthorizedEntryPoint() {
        return new UnauthorizedEntryPoint();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(final JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }
}
