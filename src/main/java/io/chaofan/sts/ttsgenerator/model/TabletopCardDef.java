package io.chaofan.sts.ttsgenerator.model;

import java.awt.Point;
import java.util.List;

public class TabletopCardDef {
    public String description;
    public String upgradeDescription;
    public float descriptionYOffset;
    public List<Point> slots;
    public List<Point> upgradeSlots;
    public int cost = Integer.MIN_VALUE;
    public int upgradeCost = Integer.MIN_VALUE;
    public String type;
    public String rarity;
}
