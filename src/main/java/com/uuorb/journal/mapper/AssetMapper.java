package com.uuorb.journal.mapper;

import com.uuorb.journal.model.Asset;
import com.uuorb.journal.model.AssetRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssetMapper {
    List<Asset> queryList(String userId);
    Asset queryById(String assetId);
    Integer insert(Asset asset);
    void update(Asset asset);
    @Delete("DELETE FROM asset WHERE asset_id = #{assetId} AND user_id = #{userId}")
    void delete(String assetId, String userId);
    
    List<AssetRecord> queryRecords(String assetId);
    void insertRecord(AssetRecord record);

    @Delete("DELETE FROM asset_record WHERE user_id = #{userId}")
    void deleteRecordByUserId(String userId);

    @Delete("DELETE FROM asset WHERE user_id = #{userId}")
    void deleteByUserId(String userId);
}
