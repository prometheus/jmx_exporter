package io.prometheus.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static javax.management.openmbean.SimpleType.BIGINTEGER;
import static javax.management.openmbean.SimpleType.DOUBLE;
import static javax.management.openmbean.SimpleType.STRING;

/**
 * Simple tabular MBean example exposing the following tables:
 * <pre>
 *   Server 1 Disk Usage Table
 *   -------------------------
 *   Filesystem     Size    Used   Avail  Use%  Mounted on
 *   /dev/sda1      7G      6G     1G     86%   /home
 *   /dev/sda2      14G     8G     6G     57%   /
 *
 *   Server 2 Disk Usage Table
 *   -------------------------
 *   Filesystem     Size    Used   Avail  Use%  Mounted on
 *   /dev/sda1      24G     13G    11G    54%   /home
 *   /dev/sda2      100G    80G    20G    80%   /
 * </pre>
 */
public class TabularMBean implements DynamicMBean {

  private final MBeanInfo mBeanInfo;
  private final Map<String, TabularDataSupport> data;

  public TabularMBean() throws OpenDataException {

    String[] columnNames = { "source", "target", "size", "used", "avail", "pcent" };
    String[] columnDescriptions = { "filesystem", "mounted on", "size", "used", "available", "use %" };
    OpenType<?>[] columnTypes = { STRING, STRING, BIGINTEGER, BIGINTEGER, BIGINTEGER, DOUBLE };
    CompositeType rowType = new CompositeType("Disk Usage Row", "Row Type for File System Disk Space Usage Tables",
        columnNames, columnDescriptions, columnTypes);

    TabularType tabularType = new TabularType("Disk Usage Table", "Tabular Type for File System Disk Space Usage Tables", rowType, new String[] { "source" });
    MBeanAttributeInfo server1info = new OpenMBeanAttributeInfoSupport("Server 1 Disk Usage Table", "File System Disk Space Usage of Server 1", tabularType, true, false, false);
    MBeanAttributeInfo server2info = new OpenMBeanAttributeInfoSupport("Server 2 Disk Usage Table", "File System Disk Space Usage of Server 2", tabularType, true, false, false);

    mBeanInfo = new MBeanInfo(getClass().getName(),
        "File System Disk Usages",
        new MBeanAttributeInfo[] {server1info, server2info},
        null,
        null,
        null);

    data = new HashMap<String, TabularDataSupport>();
    data.put("Server 1 Disk Usage Table", generateServer1Data(tabularType, rowType, columnNames));
    data.put("Server 2 Disk Usage Table", generateServer2Data(tabularType, rowType, columnNames));
  }

  private TabularDataSupport generateServer1Data(TabularType tabularType, CompositeType rowType, String[] columnNames) throws OpenDataException {
    TabularDataSupport tableData = new TabularDataSupport(tabularType);
    tableData.put(new CompositeDataSupport(rowType, columnNames,
        rowData("/dev/sda1", "/home", 7, 6)));
    tableData.put(new CompositeDataSupport(rowType, columnNames,
        rowData("/dev/sda2", "/", 14, 8)));
    return tableData;
  }

  private TabularDataSupport generateServer2Data(TabularType tabularType, CompositeType rowType, String[] columnNames) throws OpenDataException {
    TabularDataSupport tableData = new TabularDataSupport(tabularType);
    tableData.put(new CompositeDataSupport(rowType, columnNames,
        rowData("/dev/sda1", "/home", 24, 13)));
    tableData.put(new CompositeDataSupport(rowType, columnNames,
        rowData("/dev/sda2", "/", 100, 80)));
    return tableData;
  }

  private Object[] rowData(String fs, String mount, int size, int used) {
    BigInteger gigaByte = BigInteger.valueOf(1024).pow(3);
    return new Object[]{fs, mount,
        BigInteger.valueOf(size).multiply(gigaByte),
        BigInteger.valueOf(used).multiply(gigaByte),
        BigInteger.valueOf(size-used).multiply(gigaByte),
        (double) used / (double) size};
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return mBeanInfo;
  }

  @Override
  public Object getAttribute(String attribute) throws AttributeNotFoundException {
    if (data.containsKey(attribute)) {
      return data.get(attribute);
    }
    throw new AttributeNotFoundException("MBean attribute " + attribute + " not exposed for " + getClass().getName());
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    AttributeList values = new AttributeList(attributes.length);
    for (String attributeName : attributes) {
      Object result;
      try {
        result = getAttribute(attributeName);
      } catch (Exception e) {
        result = e;
      }
      values.add(new Attribute(attributeName, result));
    }
    return values;
  }

  @Override
  public void setAttribute(Attribute attribute) throws MBeanException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) {
    throw new UnsupportedOperationException("not implemented");
  }
}