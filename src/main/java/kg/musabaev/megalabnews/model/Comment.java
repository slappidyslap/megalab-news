package kg.musabaev.megalabnews.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString(exclude = {"parent", "post", "commentator"})
@NoArgsConstructor
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id", nullable = false)
	Long id;

	@ManyToOne(
			fetch = FetchType.LAZY,
			cascade = {
					CascadeType.MERGE, CascadeType.PERSIST,
					CascadeType.PERSIST, CascadeType.REFRESH
			},
			optional = false)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	Post post;

	@OneToOne(
			fetch = FetchType.LAZY,
			cascade = {
					CascadeType.MERGE, CascadeType.PERSIST,
					CascadeType.PERSIST, CascadeType.REFRESH
			})
	@JoinColumn(name = "parent_comment_id", updatable = false)
	@Nullable
	Comment parent;

	@Column(name = "commentator_id", nullable = false, updatable = false)
	Long commentator;

	@Column(nullable = false, length = 2000)
	String content;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	LocalDate createdDate;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Comment comment = (Comment) o;
		return id != null && Objects.equals(id, comment.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
