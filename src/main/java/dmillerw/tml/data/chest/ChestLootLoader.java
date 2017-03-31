
package dmillerw.tml.data.chest;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

import dmillerw.tml.TooMuchLoot;
import dmillerw.tml.data.LootGroupXML;
import dmillerw.tml.data.LootLoadingMode;
import dmillerw.tml.data.XMLDataBridge;
import dmillerw.tml.helper.LogHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;


/**
 * @author dmillerw
 */
public class ChestLootLoader
{

  public static FilenameFilter FILTER = new FilenameFilter(){
    @Override
    public boolean accept( File file, String s )
    {
      return s.endsWith( ".xml" );
    }
  };

  public static HashMap<String, ChestGenHooks> copyLootTable( Map<String, ChestGenHooks> lootTable )
  {
    HashMap<String, ChestGenHooks> newLootTable = Maps.newHashMap();
    try
    {
      for( String key : lootTable.keySet() )
      {
        ChestGenHooks old = lootTable.get( key );
        List<WeightedRandomChestContent> copiedList = Lists.newArrayList();
        for( WeightedRandomChestContent chestContent : (List<WeightedRandomChestContent>) TooMuchLoot.contents.get( old ) )
        {
          copiedList.add( new WeightedRandomChestContent( chestContent.theItemId.copy(), chestContent.theMinimumChanceToGenerateItem, chestContent.theMaximumChanceToGenerateItem, chestContent.itemWeight ) );
        }
        newLootTable.put( key, new ChestGenHooks( key, copiedList.toArray( new WeightedRandomChestContent[copiedList.size()] ), old.getMin(), old.getMax() ) );
      }
    }
    catch( Exception ex )
    {
    }
    return newLootTable;
  }

  public static void restoreCachedLootTable()
  {
    try
    {
      TooMuchLoot.chestInfo.set( ChestGenHooks.class, ChestLootLoader.copyLootTable( (Map<String, ChestGenHooks>) TooMuchLoot.chestInfo.get( ChestGenHooks.class ) ) );
    }
    catch( Exception ex )
    {
    }
  }

  public static void generateFiles( File generationDir, String... categories )
  {
    if( !generationDir.exists() )
    {
      generationDir.mkdirs();
    }

    if( categories == null || categories.length == 0 )
    {
      categories = TooMuchLoot.chestGenCategories;
    }

    for( String key : categories )
    {
      try
      {
        String sanitizedKey = key.replaceAll( "[^a-zA-Z0-9\\._]+", "_" );

        File file = new File( generationDir, sanitizedKey + ".xml" );
        if( file.exists() )
          file.delete();

        ChestLootCategory lootCategory = ChestLootCategory.fromChestGenHooks( key );
        lootCategory.loading_mode = LootLoadingMode.OVERRIDE;
        for( ChestLootItem lootItem : lootCategory.loot )
        {
          if( lootItem.nbt == null || ( lootItem.nbt != null && lootItem.nbt.hasNoTags() ) )
            lootItem.nbt = null;
        }

        LootGroupXML tXMLObject = XMLDataBridge.parseToXMLObject( lootCategory );
        try
        {
          JAXBContext tJaxbCtx = JAXBContext.newInstance( LootGroupXML.class );
          Marshaller jaxMarsh = tJaxbCtx.createMarshaller();
          jaxMarsh.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
          jaxMarsh.marshal( tXMLObject, new FileOutputStream( file, false ) );
        }
        catch( Exception e )
        {
          LogHelper.warn( String.format( "Error while creating lootfile %s", e.getMessage() ), false );
          e.printStackTrace();
        }
      }
      catch( Exception ex )
      {
        ex.printStackTrace();
      }
    }
  }

  public static void loadFiles( File scanDir )
  {
    SetMultimap<String, WeightedRandomChestContent> toAdd = HashMultimap.create();
    SetMultimap<String, ItemStack> toRemove = HashMultimap.create();
    Map<String, ChestGenHooks> tempMap = Maps.newHashMap();

    for( File file : scanDir.listFiles( FILTER ) )
    {
      LogHelper.logParse( file.getName() );

      try
      {
        JAXBContext tJaxbCtx = JAXBContext.newInstance( LootGroupXML.class );
        Unmarshaller jaxUnmarsh = tJaxbCtx.createUnmarshaller();

        LootGroupXML tXMLObject = null;
        tXMLObject = (LootGroupXML) jaxUnmarsh.unmarshal( file );

        ChestLootCategory lootCategory = XMLDataBridge.parseToChestLootCat( tXMLObject );

        for( ChestLootItem chestLootItem : lootCategory.loot )
        {
          chestLootItem.checkCountValues();
        }

        if( lootCategory.loading_mode == LootLoadingMode.OVERRIDE )
        {
          if( !tempMap.containsKey( lootCategory.category ) )
          {
            LogHelper.logOverride( lootCategory.category );
            tempMap.put( lootCategory.category, lootCategory.toChestGenHooks() );
          }
          else
          {
            LogHelper.logOverrideError( lootCategory.category, file.getName() );
          }
        }
        else if( lootCategory.loading_mode == LootLoadingMode.ADD )
        {
          for( ChestLootItem lootItem : lootCategory.loot )
          {
            toAdd.put( lootCategory.category, lootItem.toChestContent() );
          }
        }
        else if( lootCategory.loading_mode == LootLoadingMode.REMOVE )
        {
          for( ChestLootItem lootItem : lootCategory.loot )
          {
            toRemove.put( lootCategory.category, lootItem.toItemStack() );
          }
        }
      }
      catch( Exception ex )
      {
        ex.printStackTrace();
      }
    }

    try
    {
      if( !tempMap.isEmpty() )
      {
        ( (HashMap<String, ChestGenHooks>) TooMuchLoot.chestInfo.get( ChestGenHooks.class ) ).putAll( tempMap );
      }
    }
    catch( Exception ex )
    {
      ex.printStackTrace();
    }

    for( Map.Entry<String, WeightedRandomChestContent> entry : toAdd.entries() )
    {
      LogHelper.logAddition( entry.getKey(), entry.getValue().theItemId.getDisplayName() );
      ChestGenHooks.getInfo( entry.getKey() ).addItem( entry.getValue() );
    }

    for( Map.Entry<String, ItemStack> entry : toRemove.entries() )
    {
      LogHelper.logRemoval( entry.getKey(), entry.getValue().getDisplayName() );
      ChestGenHooks.getInfo( entry.getKey() ).removeItem( entry.getValue() );
    }
  }
}