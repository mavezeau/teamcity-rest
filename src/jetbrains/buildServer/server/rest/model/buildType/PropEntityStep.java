package jetbrains.buildServer.server.rest.model.buildType;

import javax.xml.bind.annotation.XmlRootElement;
import jetbrains.buildServer.serverSide.ParametersDescriptor;

/**
 * @author Yegor.Yarko
 *         Date: 05.01.12
 */
@XmlRootElement(name = "step")
public class PropEntityStep extends PropEntity{
  public PropEntityStep() {
  }
  public PropEntityStep(ParametersDescriptor descriptor) {
    super(descriptor);
  }
}
