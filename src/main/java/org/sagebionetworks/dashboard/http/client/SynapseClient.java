package org.sagebionetworks.dashboard.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.sagebionetworks.dashboard.context.DashboardContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("synapseClient")
public class SynapseClient {

    @Resource
    private DashboardContext dashboardContext;

    public SynapseClient() {
        client = new DefaultHttpClient();
    }

    /**
     * @return Session token
     */
    public String login() {

        String usr = dashboardContext.getSynapseUser();
        String pwd = dashboardContext.getSynapsePassword();
        String login = "{\"email\":\"" + usr + "\", \"password\":\"" + pwd + "\"}";
        HttpEntity entity = new StringEntity(login, ContentType.APPLICATION_JSON);
        HttpPost post = new HttpPost(AUTH_LOGIN);
        post.setEntity(entity);
        JsonNode root = executeRequest(post);
        return readText(root, "sessionToken");
    }

    public String getUserName(final String userId, final String session) {

        String uri = REPO + "/userProfile/" + userId;
        HttpGet get = new HttpGet(uri);
        get.addHeader(new BasicHeader("sessionToken", session));
        JsonNode root = executeRequest(get);
        return readText(root, "userName");
    }

    public String getEntityName(final String entityId, final String session) {

        String uri = REPO + "/entity/" + entityId + "/type";
        HttpGet get = new HttpGet(uri);
        get.addHeader(new BasicHeader("sessionToken", session));
        JsonNode root = executeRequest(get);
        return readText(root, "name");
    }

    public Long getTeamId(final String teamName, final String session) {

        String uri = REPO + "/teams?fragment=" + teamName;
        HttpGet get = new HttpGet(uri);
        get.addHeader(new BasicHeader("sessionToken", session));
        JsonNode root = executeRequest(get);
        Iterator<JsonNode> iterator = root.get("results").elements();
        JsonNode team = null;
        while (iterator.hasNext()) {
            team = iterator.next();
            if (teamName.equals(team.get("name").asText())) {
                return team.get("id").asLong();
            }
        }
        return null;
    }

    public boolean isMemberOfTeam(final String userId, final Long teamId, final String session) {

        if (userId == null) {
            return false;
        }
        String uri = REPO + "/team/" + teamId + "/member/" + userId + "/membershipStatus";
        HttpGet get = new HttpGet(uri);
        get.addHeader(new BasicHeader("sessionToken", session));
        JsonNode root = executeRequest(get);
        return root.get("isMember").asBoolean();
    }

    public List<SynapseUser> getUsers(long offset, long limit) {
        List<SynapseUser> users = new ArrayList<SynapseUser>();
        String uri = REPO + "/user?offset=" + offset + "&limit=" + limit;
        HttpGet get = new HttpGet(uri);
        JsonNode node = executeRequest(get);
        Iterator<JsonNode> iterator = node.get("results").elements();
        while(iterator.hasNext()) {
            JsonNode userNode = iterator.next();
            String userId = readText(userNode, "ownerId");
            String userName = readText(userNode, "userName");
            String email = null;
            JsonNode emails = userNode.get("emails");
            if (emails != null && emails.size() > 0) {
                    email = emails.get(0).asText();
            }
            String firstName = readText(userNode, "firstName");
            String lastName = readText(userNode, "lastName");
            SynapseUser user = new SynapseUser(userId, userName, email, firstName, lastName);
            users.add(user);
        }
        return users;
    }

    private JsonNode executeRequest(HttpUriRequest request) {
        InputStream inputStream = null;
        try {
            HttpResponse response = client.execute(request);
            if (HttpStatus.SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
                throw new UnauthorizedException();
            }
            if (HttpStatus.SC_FORBIDDEN == response.getStatusLine().getStatusCode()) {
                throw new ForbiddenException();
            }
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readValue(inputStream, JsonNode.class);
            return root;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String readText(JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.get(fieldName);
        return (value == null ? null : value.asText());
    }

    private static final String AUTH = "https://repo-prod.prod.sagebase.org/auth/v1";
    private static final String AUTH_LOGIN = AUTH + "/session";
    private static final String REPO = "https://repo-prod.prod.sagebase.org/repo/v1";

    private final HttpClient client;
}
