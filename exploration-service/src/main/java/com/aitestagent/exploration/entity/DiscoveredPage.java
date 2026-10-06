package com.aitestagent.exploration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a single page discovered and scraped during an exploration crawl.
 *
 * <p>Structured collections (links, buttons, inputs, forms) are serialised as JSON strings
 * so that the schema remains flat while preserving all detail needed for LLM prompting.</p>
 */
@Entity
@Table(name = "discovered_pages")
public class DiscoveredPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The parent exploration session that discovered this page. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exploration_id", nullable = false)
    private Exploration exploration;

    @Column(nullable = false, length = 2048)
    private String pageUrl;

    @Column(length = 512)
    private String pageTitle;

    /**
     * JSON array of discovered internal/external href values, e.g.:
     * ["/login", "/products", "https://external.com"]
     */
    @Column(columnDefinition = "TEXT")
    private String discoveredLinks;

    /**
     * JSON array of button descriptors, e.g.:
     * [{"text":"Login","selector":"button#login-btn"},{"text":"Sign Up","selector":"button.signup"}]
     */
    @Column(columnDefinition = "TEXT")
    private String buttons;

    /**
     * JSON array of input field descriptors, e.g.:
     * [{"id":"email","name":"email","type":"email","placeholder":"Enter your email"}]
     */
    @Column(columnDefinition = "TEXT")
    private String inputs;

    /**
     * JSON array of form descriptors, each containing the form's id/action and its child inputs, e.g.:
     * [{"formId":"login-form","action":"/login","fields":[{"name":"email","type":"email"},{"name":"password","type":"password"}]}]
     */
    @Column(columnDefinition = "TEXT")
    private String forms;

    /**
     * Plain-text description of how this page connects to others in the application.
     * Example: "Login (/login) → links to → Dashboard (/dashboard), Register (/register)"
     */
    @Column(columnDefinition = "TEXT")
    private String navigationFlow;

    public DiscoveredPage() {
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Exploration getExploration() {
        return exploration;
    }

    public void setExploration(Exploration exploration) {
        this.exploration = exploration;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getDiscoveredLinks() {
        return discoveredLinks;
    }

    public void setDiscoveredLinks(String discoveredLinks) {
        this.discoveredLinks = discoveredLinks;
    }

    public String getButtons() {
        return buttons;
    }

    public void setButtons(String buttons) {
        this.buttons = buttons;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public String getForms() {
        return forms;
    }

    public void setForms(String forms) {
        this.forms = forms;
    }

    public String getNavigationFlow() {
        return navigationFlow;
    }

    public void setNavigationFlow(String navigationFlow) {
        this.navigationFlow = navigationFlow;
    }
}
