package jetbrains.buildServer.server.rest.data;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.MultiValuesMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.server.rest.errors.LocatorProcessException;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class that support parsing of "locators".
 * Locator is a string with single value or several named "dimensions".
 * Example:
 * <tt>31</tt> - locator wth single value "31"
 * <tt>name:Frodo</tt> - locator wth single dimension "name" which has value "Frodo"
 * <tt>name:Frodo,age:14</tt> - locator with two dimensions "name" which has value "Frodo" and "age", which has value "14"
 * <tt>text:(Freaking symbols:,name)</tt> - locator with single dimension "text" which has value "Freaking symbols:,name"
 * <p/>
 * Dimension name should be is alpha-numeric. Dimension value should not contain symbol "," if not enclosed in "(" and ")" or
 * should not contain symbol ")" (if enclosed in "(" and ")")
 *
 * @author Yegor.Yarko
 *         Date: 13.08.2010
 */
public class Locator {
  private static final Logger LOG = Logger.getInstance(Locator.class.getName());
  private static final String DIMENSION_NAME_VALUE_DELIMITER = ":";
  private static final String DIMENSIONS_DELIMITER = ",";
  private static final String DIMENSION_COMPLEX_VALUE_START_DELIMITER = "(";
  private static final String DIMENSION_COMPLEX_VALUE_END_DELIMITER = ")";
  public static final String LOCATOR_SINGLE_VALUE_UNUSED_NAME = "locator_single_value";

  private final MultiValuesMap<String, String> myDimensions;
  private final String mySingleValue;

  private final Set<String> myUnusedDimensions;

  public Locator(@Nullable final String locator) throws LocatorProcessException{
    if (StringUtil.isEmpty(locator)) {
      throw new LocatorProcessException("Invalid locator. Cannot be empty.");
    }
    @SuppressWarnings("ConstantConditions")final boolean hasDimensions = locator.contains(DIMENSION_NAME_VALUE_DELIMITER);
    if (!hasDimensions) {
      mySingleValue = locator;
      myDimensions = new MultiValuesMap<String, String>();
      myUnusedDimensions = new HashSet<String>(Collections.singleton(LOCATOR_SINGLE_VALUE_UNUSED_NAME));
    } else {
      mySingleValue = null;
      myDimensions = parse(locator);
      myUnusedDimensions = new HashSet<String>(myDimensions.keySet());
    }
  }

  private static MultiValuesMap<String, String> parse(final String locator) {
    MultiValuesMap<String, String> result = new MultiValuesMap<String, String>();
    String currentDimensionName;
    String currentDimensionValue;
    int parsedIndex = 0;
    while (parsedIndex < locator.length()) {
      int nameEnd = locator.indexOf(DIMENSION_NAME_VALUE_DELIMITER, parsedIndex);
      if (nameEnd == parsedIndex || nameEnd == -1) {
        throw new LocatorProcessException(locator, parsedIndex, "Could not find '" + DIMENSION_NAME_VALUE_DELIMITER + "'");
      }
      currentDimensionName = locator.substring(parsedIndex, nameEnd);
      if (!isValidName(currentDimensionName)){
        throw new LocatorProcessException(locator, parsedIndex, "Invalid dimension name :'" + currentDimensionName + "'. Should contain only alpha-numeric symbols");
      }
      final String valueAndRest = locator.substring(nameEnd + DIMENSION_NAME_VALUE_DELIMITER.length());
      if (valueAndRest.startsWith(DIMENSION_COMPLEX_VALUE_START_DELIMITER)) {
        //complex value detected
        final int complexValueEnd =
          valueAndRest.indexOf(DIMENSION_COMPLEX_VALUE_END_DELIMITER, DIMENSION_COMPLEX_VALUE_START_DELIMITER.length());
        if (complexValueEnd == -1) {
          throw new LocatorProcessException(locator, nameEnd + DIMENSION_NAME_VALUE_DELIMITER.length() +
                                                     DIMENSION_COMPLEX_VALUE_START_DELIMITER.length(),
                                            "Could not find '" + DIMENSION_COMPLEX_VALUE_END_DELIMITER + "'");
        }
        currentDimensionValue = valueAndRest.substring(DIMENSION_COMPLEX_VALUE_START_DELIMITER.length(), complexValueEnd);
        parsedIndex = nameEnd + DIMENSION_NAME_VALUE_DELIMITER.length() + complexValueEnd + DIMENSION_COMPLEX_VALUE_END_DELIMITER.length();
        if (parsedIndex != locator.length()) {
          if (!locator.startsWith(DIMENSIONS_DELIMITER, parsedIndex)) {
            throw new LocatorProcessException(locator, parsedIndex,
                                              "No dimensions delimiter " + DIMENSIONS_DELIMITER + " after complex value");
          } else {
            parsedIndex += DIMENSIONS_DELIMITER.length();
          }
        }
      } else {
        int valueEnd = valueAndRest.indexOf(DIMENSIONS_DELIMITER);
        if (valueEnd == -1) {
          currentDimensionValue = valueAndRest;
          parsedIndex = locator.length();
        } else {
          currentDimensionValue = valueAndRest.substring(0, valueEnd);
          parsedIndex = nameEnd + DIMENSION_NAME_VALUE_DELIMITER.length() + valueEnd + DIMENSIONS_DELIMITER.length();
        }
      }
      result.put(currentDimensionName, currentDimensionValue);
    }

    return result;
  }

  private static boolean isValidName(final String name) {
    for (int i = 0; i < name.length(); i++) {
      if (!Character.isLetter(name.charAt(i)) && !Character.isDigit(name.charAt(i))) return false;
    }
    return true;
  }

  //todo: use this whenever possible
  public void checkLocatorFullyProcessed() {
    final Set<String> unusedDimensions = getUnusedDimensions();
    if (unusedDimensions.size() > 0){
      String reportKindString = TeamCityProperties.getProperty("rest.report.unused.locator", "error");
      if (!TeamCityProperties.getBooleanOrTrue("rest.report.locator.errors")){
        reportKindString = "off";
      }
      if (!reportKindString.equals("off")){
        String message;
        if (unusedDimensions.size() > 1){
          message = "Locator dimensions " + unusedDimensions + " are ignored or unknown.";
        }else{
          if (!unusedDimensions.contains(LOCATOR_SINGLE_VALUE_UNUSED_NAME)){
            message = "Locator dimension " + unusedDimensions + " is ignored or unknown.";
          }else{
            message = "Single value locator is not supported here.";
          }
        }
        if (reportKindString.contains("log")) {
          if (reportKindString.contains("log-warn")) {
            LOG.warn(message);
          } else {
            LOG.debug(message);
          }
        }
        if (reportKindString.equals("error")){
          throw new LocatorProcessException(message);
        }
      }
    }
  }

  public boolean isSingleValue() {
    return mySingleValue != null;
  }

  /**
   * @return locator's not-null value if it is single-value locator, 'null' otherwise
   */
  @Nullable
  public String getSingleValue() {
    myUnusedDimensions.remove(LOCATOR_SINGLE_VALUE_UNUSED_NAME);
    return mySingleValue;
  }

  @Nullable
  public Long getSingleValueAsLong() {
    final String singleValue = getSingleValue();
    if (singleValue == null) {
      return null;
    }
    try {
      return Long.parseLong(singleValue);
    } catch (NumberFormatException e) {
      throw new LocatorProcessException("Invalid single value: " + singleValue + ". Should be a number.");
    }
  }

  @Nullable
  public Long getSingleDimensionValueAsLong(@NotNull final String dimensionName) {
    final String value = getSingleDimensionValue(dimensionName);
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new LocatorProcessException("Invalid value of dimension '" + dimensionName + "': " + value + ". Should be a number.");
    }
  }

  @Nullable
  public Boolean getSingleDimensionValueAsBoolean(@NotNull final String dimensionName) {
    final String value = getSingleDimensionValue(dimensionName);
    if (value == null || "all".equalsIgnoreCase(value) || "any".equalsIgnoreCase(value)){
      return null;
    }
    if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "in".equalsIgnoreCase(value)){
      return true;
    }
    if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "out".equalsIgnoreCase(value)){
      return false;
    }
    throw new LocatorProcessException("Invalid value of dimension '" + dimensionName + "': " + value + ". Should be 'true', 'false' or 'any'.");
  }

  /**
   *
   * @param dimensionName name of the dimension
   * @param defaultValue default value to use if no dimension with the name is found
   * @return value specified by the dimension with name "dimensionName" (one of the possible values can be "null") or
   * "defaultValue" if such dimension is not present
   */
  @Nullable
  public Boolean getSingleDimensionValueAsBoolean(@NotNull final String dimensionName, @Nullable Boolean defaultValue) {
    final String value = getSingleDimensionValue(dimensionName);
    if (value == null){
      return defaultValue;
    }
    return getSingleDimensionValueAsBoolean(dimensionName);
  }

  /**
   * Extracts the single dimension value from dimensions.
   *
   * @param dimensionName the name of the dimension to extract value.   @return 'null' if no such dimension is found, value of the dimension otherwise.
   * @throws jetbrains.buildServer.server.rest.errors.LocatorProcessException
   *          if there are more then a single dimension definition for a 'dimensionName' name or the dimension has no value specified.
   */
  @Nullable
  public String getSingleDimensionValue(@NotNull final String dimensionName) {
    Collection<String> idDimension = myDimensions.get(dimensionName);
    if (idDimension == null || idDimension.size() == 0) {
      return null;
    }
    myUnusedDimensions.remove(dimensionName);
    if (idDimension.size() > 1) {
      throw new LocatorProcessException("Only single '" + dimensionName + "' dimension is supported in locator. Found: " + idDimension);
    }
    return idDimension.iterator().next();
  }

  public int getDimensionsCount() {
    return myDimensions.keySet().size();
  }

  /**
   * Should be used only for multi-dimension locators
   * @param name name of the dimension
   * @param value value of the dimension
   */
  public void setDimension(@NotNull final String name, @NotNull final String value) {
    final Collection<String> oldValues = myDimensions.removeAll(name);
    myDimensions.put(name, value);

    if (oldValues == null || oldValues.size() == 0){
      myUnusedDimensions.add(name);
    }
  }

  /**
   * Provides the names of dimensions whose values were never retrieved
   * @return names of the dimensions not yet queried
   */
  @NotNull
  public Set<String> getUnusedDimensions() {
    return myUnusedDimensions;
  }

  /**
   *  Returns a locator based on the supplied one replacing the numeric value of the dimention specified with the passed number.
   *  Only a limited subset of locators is supported (e.g. no brackets)
   * @param locator existing locator, should be valid!
   * @param dimensionName only alpha-numeric characters are supported! Only numeric vaues withour brackets are supported!
   * @param value new value for the dimention, only alpha-numeric characters are supported!
   * @return
   */
  public static String setDimension(final String locator, final String dimensionName, final long value) {
    final Matcher matcher = Pattern.compile(dimensionName + DIMENSION_NAME_VALUE_DELIMITER + "\\d+").matcher(locator);
    String result = matcher.replaceFirst(dimensionName + DIMENSION_NAME_VALUE_DELIMITER + Long.toString(value));
    try {
      matcher.end();
    } catch (IllegalStateException e) {
      result = locator + DIMENSIONS_DELIMITER + dimensionName + DIMENSION_NAME_VALUE_DELIMITER + Long.toString(value);
    }
    return result;
  }
}
