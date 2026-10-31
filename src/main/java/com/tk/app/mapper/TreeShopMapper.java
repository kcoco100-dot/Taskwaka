package com.tk.app.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.tk.app.service.TreeShopService.Item;

@Mapper
public interface TreeShopMapper {
    List<Item> catalog(@Param("work") long work);
    long balance(@Param("work") long work);
    Long lastLog(@Param("work") long work);
    long transactionId();
    void transaction(@Param("id") long id, @Param("type") String type, @Param("amount") long amount, @Param("log") Long log);
    void credit(@Param("work") long work, @Param("log") long log, @Param("delta") long delta, @Param("total") long total);
    int charge(@Param("work") long work, @Param("price") long price);
    void purchase(@Param("work") long work,@Param("member") long member,@Param("item") long item,@Param("price") long price,@Param("tx") long tx);
    void unequip(@Param("work") long work,@Param("item") long owned);
    void clearSlot(@Param("work") long work,@Param("slot") String slot);
    void equip(@Param("work") long work,@Param("member") long member,@Param("owned") long owned);
}
