package com.moona.productsmanager.moonaproductsmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {
    private ProductType productType = new ProductType();
    private Attributes attributes = new Attributes();
    private Collections collections = new Collections();
    private Categories categories = new Categories();

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public Collections getCollections() {
        return collections;
    }

    public void setCollections(Collections collections) {
        this.collections = collections;
    }

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public static class ProductType {
        private String groceryId;

        public String getGroceryId() {
            return groceryId;
        }

        public void setGroceryId(String groceryId) {
            this.groceryId = groceryId;
        }
    }

    public static class Attributes {
        private String productMinAmountId;
        private String productWeightedId;
        private String boycottId;
        private String boxSizeId;
        private String minOrderQuantityId;
        private String providerId;
        private String providersId;
        private String minStockQuantityId;

        public String getProductMinAmountId() {
            return productMinAmountId;
        }

        public void setProductMinAmountId(String productMinAmountId) {
            this.productMinAmountId = productMinAmountId;
        }

        public String getProductWeightedId() {
            return productWeightedId;
        }

        public void setProductWeightedId(String productWeightedId) {
            this.productWeightedId = productWeightedId;
        }

        public String getBoycottId() {
            return boycottId;
        }

        public void setBoycottId(String boycottId) {
            this.boycottId = boycottId;
        }

        public String getBoxSizeId() {
            return boxSizeId;
        }

        public void setBoxSizeId(String boxSizeId) {
            this.boxSizeId = boxSizeId;
        }

        public String getMinOrderQuantityId() {
            return minOrderQuantityId;
        }

        public void setMinOrderQuantityId(String minOrderQuantityId) {
            this.minOrderQuantityId = minOrderQuantityId;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getProvidersId() {
            return providersId;
        }

        public void setProvidersId(String providersId) {
            this.providersId = providersId;
        }

        public String getMinStockQuantityId() {
            return minStockQuantityId;
        }

        public void setMinStockQuantityId(String minStockQuantityId) {
            this.minStockQuantityId = minStockQuantityId;
        }
    }

    public static class Collections {
        private String ramallahNewArrivalsId;
        private String arrabaNewArrivalsId;
        private String ramadanId;
        private String eidCakeId;
        private String jeningOffersId;
        private String ramallahOffersId;
        private String zainBakeryId;

        public String getRamallahNewArrivalsId() {
            return ramallahNewArrivalsId;
        }

        public void setRamallahNewArrivalsId(String ramallahNewArrivalsId) {
            this.ramallahNewArrivalsId = ramallahNewArrivalsId;
        }

        public String getArrabaNewArrivalsId() {
            return arrabaNewArrivalsId;
        }

        public void setArrabaNewArrivalsId(String arrabaNewArrivalsId) {
            this.arrabaNewArrivalsId = arrabaNewArrivalsId;
        }

        public String getRamadanId() {
            return ramadanId;
        }

        public void setRamadanId(String ramadanId) {
            this.ramadanId = ramadanId;
        }

        public String getEidCakeId() {
            return eidCakeId;
        }

        public void setEidCakeId(String eidCakeId) {
            this.eidCakeId = eidCakeId;
        }

        public String getJeningOffersId() {
            return jeningOffersId;
        }

        public void setJeningOffersId(String jeningOffersId) {
            this.jeningOffersId = jeningOffersId;
        }

        public String getRamallahOffersId() {
            return ramallahOffersId;
        }

        public void setRamallahOffersId(String ramallahOffersId) {
            this.ramallahOffersId = ramallahOffersId;
        }

        public String getZainBakeryId() {
            return zainBakeryId;
        }

        public void setZainBakeryId(String zainBakeryId) {
            this.zainBakeryId = zainBakeryId;
        }
    }

    public static class Categories {
        private String mixId;
        private String fruitId;
        private String moonaId;
        private String riceId;
        private String meatId;
        private String oilId;
        private String saladsId;
        private String milkId;
        private String breadId;
        private String sweetsId;
        private String cleanId;
        private String drinksId;
        private String animalsId;
        private String personalCareId;
        private String beansId;
        private String frozenId;
        private String chipsId;
        private String sauceId;
        private String bharId;
        private String teaId;
        private String healthyId;
        private String papersId;
        private String childrenId;
        private String waterId;
        private String plasticId;

        public String getMixId() {
            return mixId;
        }

        public void setMixId(String mixId) {
            this.mixId = mixId;
        }

        public String getFruitId() {
            return fruitId;
        }

        public void setFruitId(String fruitId) {
            this.fruitId = fruitId;
        }

        public String getMoonaId() {
            return moonaId;
        }

        public void setMoonaId(String moonaId) {
            this.moonaId = moonaId;
        }

        public String getRiceId() {
            return riceId;
        }

        public void setRiceId(String riceId) {
            this.riceId = riceId;
        }

        public String getMeatId() {
            return meatId;
        }

        public void setMeatId(String meatId) {
            this.meatId = meatId;
        }

        public String getOilId() {
            return oilId;
        }

        public void setOilId(String oilId) {
            this.oilId = oilId;
        }

        public String getSaladsId() {
            return saladsId;
        }

        public void setSaladsId(String saladsId) {
            this.saladsId = saladsId;
        }

        public String getMilkId() {
            return milkId;
        }

        public void setMilkId(String milkId) {
            this.milkId = milkId;
        }

        public String getBreadId() {
            return breadId;
        }

        public void setBreadId(String breadId) {
            this.breadId = breadId;
        }

        public String getSweetsId() {
            return sweetsId;
        }

        public void setSweetsId(String sweetsId) {
            this.sweetsId = sweetsId;
        }

        public String getCleanId() {
            return cleanId;
        }

        public void setCleanId(String cleanId) {
            this.cleanId = cleanId;
        }

        public String getDrinksId() {
            return drinksId;
        }

        public void setDrinksId(String drinksId) {
            this.drinksId = drinksId;
        }

        public String getAnimalsId() {
            return animalsId;
        }

        public void setAnimalsId(String animalsId) {
            this.animalsId = animalsId;
        }

        public String getPersonalCareId() {
            return personalCareId;
        }

        public void setPersonalCareId(String personalCareId) {
            this.personalCareId = personalCareId;
        }

        public String getBeansId() {
            return beansId;
        }

        public void setBeansId(String beansId) {
            this.beansId = beansId;
        }

        public String getFrozenId() {
            return frozenId;
        }

        public void setFrozenId(String frozenId) {
            this.frozenId = frozenId;
        }

        public String getChipsId() {
            return chipsId;
        }

        public void setChipsId(String chipsId) {
            this.chipsId = chipsId;
        }

        public String getSauceId() {
            return sauceId;
        }

        public void setSauceId(String sauceId) {
            this.sauceId = sauceId;
        }

        public String getBharId() {
            return bharId;
        }

        public void setBharId(String bharId) {
            this.bharId = bharId;
        }

        public String getTeaId() {
            return teaId;
        }

        public void setTeaId(String teaId) {
            this.teaId = teaId;
        }

        public String getHealthyId() {
            return healthyId;
        }

        public void setHealthyId(String healthyId) {
            this.healthyId = healthyId;
        }

        public String getPapersId() {
            return papersId;
        }

        public void setPapersId(String papersId) {
            this.papersId = papersId;
        }

        public String getChildrenId() {
            return childrenId;
        }

        public void setChildrenId(String childrenId) {
            this.childrenId = childrenId;
        }

        public String getWaterId() {
            return waterId;
        }

        public void setWaterId(String waterId) {
            this.waterId = waterId;
        }

        public String getPlasticId() {
            return plasticId;
        }

        public void setPlasticId(String plasticId) {
            this.plasticId = plasticId;
        }
    }
}

