package kg.musabaev.megalabnews.security;

import jakarta.persistence.*;
import kg.musabaev.megalabnews.model.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString(exclude = "owner")
@EqualsAndHashCode(of = "token")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "refresh_token_id")
	Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false, unique = true)
	User owner;

	@Column(unique = true, nullable = false)
	String token;

	@Column(nullable = false)
	Instant expiryDate;

	public RefreshToken(User owner, String token, Instant expiryDate) {
		this.owner = owner;
		this.token = token;
		this.expiryDate = expiryDate;
	}
}
