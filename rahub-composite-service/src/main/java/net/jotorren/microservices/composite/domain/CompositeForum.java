package net.jotorren.microservices.composite.domain;

import java.io.Serializable;

public class CompositeForum implements Serializable {
	private static final long serialVersionUID = 6059100903956824262L;
	
	private String forumId;
	private String topicName;
	private String topicCategory;
	private String subjectId;

	public String getForumId() {
		return forumId;
	}

	public void setForumId(String forumId) {
		this.forumId = forumId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public String getTopicCategory() {
		return topicCategory;
	}

	public void setTopicCategory(String topicCategory) {
		this.topicCategory = topicCategory;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}
}
