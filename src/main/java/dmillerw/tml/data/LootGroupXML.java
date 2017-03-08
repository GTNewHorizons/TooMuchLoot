package dmillerw.tml.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;


@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement( name = "LootGroup" )
public class LootGroupXML
{
  @XmlAttribute( name = "category" )
  protected String mCategory;  
  
  @XmlAttribute( name = "loading_mode" )
  protected String mLoading_mode;  
  
  @XmlAttribute( name = "count_min" )
  protected int mCount_min;  
  
  @XmlAttribute( name = "count_max" )
  protected int mCount_max;  
  
  
  @XmlElement( name = "loot" )
  private List<LootGroupXML.LootEntry> _mLoots;
  
  private void Init()
  {
    if (_mLoots == null)
      _mLoots = new ArrayList<LootGroupXML.LootEntry>();
  }
  
  public List<LootGroupXML.LootEntry> getLoots()
  {
    Init();
    return _mLoots;
  }
  
  @XmlAccessorType( XmlAccessType.FIELD )
  @XmlType
  public static class LootEntry
  {
    @XmlAttribute( name = "item" )
    protected String mIdentifier;

    @XmlAttribute( name = "damage" )
    protected int mDamage;
    
    @XmlAttribute( name = "nbt" )
    protected String mNBTTag;

    @XmlAttribute( name = "weight" )
    protected int mWeight;

    @XmlAttribute( name = "count_min" )
    protected int mCount_min;  
    
    @XmlAttribute( name = "count_max" )
    protected int mCount_max;  

  }
}
