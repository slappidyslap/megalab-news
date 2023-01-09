package kg.musabaev.megalabnews.security;

import jakarta.servlet.http.HttpServletResponse;
import kg.musabaev.megalabnews.repository.UserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final UserRepo userRepo;
	private final TokenFilter tokenFilter;

	public SecurityConfig(UserRepo userRepo, @Lazy TokenFilter tokenFilter) {
		this.userRepo = userRepo;
		this.tokenFilter = tokenFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests()
				.requestMatchers("/api/v1/auth/authenticate", "/api/v1/auth/register").permitAll()
				.anyRequest().authenticated()
				.and()
				.exceptionHandling(registry -> {
					registry.authenticationEntryPoint((req, res, e) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED));
//					registry.accessDeniedHandler((req, res, e) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN));
				})
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.csrf().disable()
				.headers().frameOptions().disable()
				.and()
				.build();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web
				.ignoring()
				.requestMatchers("/h2-console/**", "/api-docs/**", "/swagger-ui**", "/actuator/**");
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(userDetailsService());
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
		return daoAuthenticationProvider;
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> userRepo.findByUsername(username)
				.map(UserDetailsImpl::new)
				.orElseThrow(() -> {
					throw new UsernameNotFoundException("user not found by username");
				});
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
