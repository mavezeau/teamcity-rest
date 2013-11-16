package jetbrains.buildServer.server.rest.model.agent;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jetbrains.buildServer.server.rest.ApiUrlBuilder;
import jetbrains.buildServer.server.rest.data.AgentPoolsFinder;
import jetbrains.buildServer.server.rest.model.project.Projects;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 07.11.13
 */
@XmlRootElement(name = "agentPool")
@XmlType(name = "agentPool")
@SuppressWarnings("PublicField")
public class AgentPool {
  @XmlAttribute public String href;
  @XmlAttribute public Integer id;
  @XmlAttribute public String name;
  @XmlElement public Projects projects;
  @XmlElement public Agents agents;
  /**
   * This is used only when posting a link to a project
   */
  @XmlAttribute public String locator;

  public AgentPool() {
  }

  public AgentPool(@NotNull final jetbrains.buildServer.serverSide.agentPools.AgentPool agentPool,
                   @NotNull final ApiUrlBuilder apiUrlBuilder,
                   final @NotNull AgentPoolsFinder agentPoolsFinder) {
    href = apiUrlBuilder.getHref(agentPool);
    id = agentPool.getAgentPoolId();
    name = agentPool.getName();
    projects = new Projects(agentPoolsFinder.getPoolProjects(agentPool), apiUrlBuilder);
    //todo: support agent types
    agents = new Agents(agentPoolsFinder.getPoolAgents(agentPool), apiUrlBuilder);
  }

  @NotNull
  public jetbrains.buildServer.serverSide.agentPools.AgentPool getAgentPoolFromPosted(@NotNull final AgentPoolsFinder agentPoolsFinder) {
    AgentPoolRef agentPoolRef = new AgentPoolRef();
    agentPoolRef.id = id;
    agentPoolRef.name = name;
    agentPoolRef.locator = locator;
    return agentPoolRef.getAgentPoolFromPosted(agentPoolsFinder);
  }
}