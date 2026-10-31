package com.tk.app.service;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tk.app.mapper.GrowthTreeMapper;
import com.tk.app.mapper.TreeShopMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreeShopService {
    public record Item(long itemId, String path, String slot, long price, Long ownedId, int equipped) {}
    public record State(long balance, List<Item> items) {}
    private final TreeShopMapper shop;
    private final GrowthTreeMapper trees;

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public State state(long work) {
        return new State(shop.balance(work), shop.catalog(work));
    }

    @Transactional
    public State setEquipped(long work, long member, long itemId, boolean equip) {
        var tree = trees.findGrowthTreeForUpdate(work);
        if (tree == null || trees.findGrowthLevels().stream().noneMatch(level ->
                level.getGrowthLevelId() == tree.getGrowthLevelId() && level.getLevelNo() >= 5))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "5단계부터 상점을 이용할 수 있습니다.");
        Item item = item(work, itemId);
        if (equip && item.ownedId() == null) {
            Long log = shop.lastLog(work);
            if (item.price() > 0) {
                if (log == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "지갑에 연결할 경험치 기록이 없습니다.");
                shop.credit(work, log, 0, tree.getTotalExp());
                if (shop.charge(work, item.price()) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "경험치가 부족합니다.");
            }
            long tx = shop.transactionId();
            shop.transaction(tx, "PURCHASE", -item.price(), log);
            shop.purchase(work, member, itemId, item.price(), tx);
            item = item(work, itemId);
        }
        if (equip && item.equipped() == 0) {
            if (Set.of("BACKGROUND", "FRUIT", "HAT").contains(item.slot())) shop.clearSlot(work, item.slot());
            shop.equip(work, member, item.ownedId());
        } else if (!equip && item.ownedId() != null) {
            shop.unequip(work, item.ownedId());
        }
        return state(work);
    }

    private Item item(long work, long id) {
        return shop.catalog(work).stream().filter(item -> item.itemId() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "판매 중인 상품이 아닙니다."));
    }
}
