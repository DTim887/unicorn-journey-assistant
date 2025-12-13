package com.unicorn.journey.assistant.hotel.enums;

public enum FacilityMapEnum {
    attChallengeTrails("attChallengeTrails;entityType=Attraction;destination=shdr", "古迹探索营的绳索挑战道"),
    attBuzzLightyearPlanetRescue("attBuzzLightyearPlanetRescue;entityType=Attraction;destination=shdr", "巴斯光年星际营救"),
    attFantasiaCarousel("attFantasiaCarousel;entityType=Attraction;destination=shdr", "幻想曲旋转木马"),
    attAdventuresWinniePooh("attAdventuresWinniePooh;entityType=Attraction;destination=shdr", "小熊维尼历险记"),
    attJetPacks("attJetPacks;entityType=Attraction;destination=shdr", "喷气背包飞行器"),
    attExplorerCanoes("attExplorerCanoes;entityType=Attraction;destination=shdr", "探险家独木舟"),
    attHunnyPotSpin("attHunnyPotSpin;entityType=Attraction;destination=shdr", "旋转疯蜜罐"),
    attSevenDwarfsMineTrain("attSevenDwarfsMineTrain;entityType=Attraction;destination=shdr", "七个小矮人矿山车"),
    attPeterPansFlight("attPeterPansFlight;entityType=Attraction;destination=shdr", "小飞侠天空奇遇"),
    attZootopiaHotPursuit("attZootopiaHotPursuit;entityType=Attraction;destination=shdr", "疯狂动物城：热力追踪"),
    attRoaringRapids("attRoaringRapids;entityType=Attraction;destination=shdr", "雷鸣山漂流"),
    attTronAttraction("attTronAttraction;entityType=Attraction;destination=shdr", "创极速光轮－雪佛兰呈献"),
    attVistaTrail("attVistaTrail;entityType=Attraction;destination=shdr", "古迹探索营的探索步行道"),
    attRexsRCRacer("attRexsRCRacer;entityType=Attraction;destination=shdr", "抱抱龙冲天赛车"),
    attEnchantedStorybookCastle("attEnchantedStorybookCastle;entityType=Attraction;destination=shdr", "奇幻童话城堡"),
    attSoaringOverHorizon("attSoaringOverHorizon;entityType=Attraction;destination=shdr", "翱翔•飞越地平线"),
    attPiratesOfCaribbean("attPiratesOfCaribbean;entityType=Attraction;destination=shdr", "加勒比海盗——沉落宝藏之战"),
    attWoodysRoundUp("attWoodysRoundUp;entityType=Attraction;destination=shdr", "胡迪牛仔嘉年华"),
    attVoyageToCrystalGrotto("attVoyageToCrystalGrotto;entityType=Attraction;destination=shdr", "晶彩奇航"),
    attDumboFlyingElephant("attDumboFlyingElephant;entityType=Attraction;destination=shdr", "小飞象"),
    attSlinkyDogSpin("attSlinkyDogSpin;entityType=Attraction;destination=shdr", "弹簧狗团团转");

    private final String facilityId;
    private final String name;

    FacilityMapEnum(String facilityId, String name) {
        this.facilityId = facilityId;
        this.name = name;
    }

    public String getFacilityId() { return facilityId; }
    public String getName() { return name; }

    public static String findNameByFacilityId(String facilityId) {
        for (FacilityMapEnum e : values()) {
            if (e.facilityId.equals(facilityId)) return e.name;
        }
        return null;
    }
}
