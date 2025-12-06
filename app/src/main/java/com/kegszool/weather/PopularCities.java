package com.kegszool.weather;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class PopularCities {

    private static final Map<String, String> CITIES = createCities();

    private PopularCities() {
    }

    static Map<String, String> getCities() {
        return CITIES;
    }

    private static Map<String, String> createCities() {
        Map<String, String> cities = new LinkedHashMap<>();
        cities.put("Moscow", "Москва");
        cities.put("Saint Petersburg", "Санкт-Петербург");
        cities.put("Novosibirsk", "Новосибирск");
        cities.put("Yekaterinburg", "Екатеринбург");
        cities.put("Kazan", "Казань");
        cities.put("Nizhny Novgorod", "Нижний Новгород");
        cities.put("Chelyabinsk", "Челябинск");
        cities.put("Samara", "Самара");
        cities.put("Omsk", "Омск");
        cities.put("Rostov-on-Don", "Ростов-на-Дону");
        cities.put("Ufa", "Уфа");
        cities.put("Krasnoyarsk", "Красноярск");
        cities.put("Perm", "Пермь");
        cities.put("Voronezh", "Воронеж");
        cities.put("Volgograd", "Волгоград");
        cities.put("Krasnodar", "Краснодар");
        cities.put("Sochi", "Сочи");
        cities.put("Kaliningrad", "Калининград");
        cities.put("Vladivostok", "Владивосток");
        cities.put("Murmansk", "Мурманск");
        cities.put("Khabarovsk", "Хабаровск");
        cities.put("Irkutsk", "Иркутск");
        cities.put("Yakutsk", "Якутск");
        cities.put("Astrakhan", "Астрахань");
        cities.put("Saratov", "Саратов");
        cities.put("Petropavlovsk-Kamchatsky", "Петропавловск-Камчатский");

        // Europe
        cities.put("London", "Лондон");
        cities.put("Paris", "Париж");
        cities.put("Berlin", "Берлин");
        cities.put("Rome", "Рим");
        cities.put("Madrid", "Мадрид");
        cities.put("Barcelona", "Барселона");
        cities.put("Vienna", "Вена");
        cities.put("Prague", "Прага");
        cities.put("Warsaw", "Варшава");
        cities.put("Budapest", "Будапешт");
        cities.put("Amsterdam", "Амстердам");
        cities.put("Brussels", "Брюссель");
        cities.put("Stockholm", "Стокгольм");
        cities.put("Oslo", "Осло");
        cities.put("Copenhagen", "Копенгаген");
        cities.put("Athens", "Афины");
        cities.put("Helsinki", "Хельсинки");
        cities.put("Dublin", "Дублин");
        cities.put("Zurich", "Цюрих");
        cities.put("Lisbon", "Лиссабон");

        // Asia
        cities.put("Tokyo", "Токио");
        cities.put("Seoul", "Сеул");
        cities.put("Beijing", "Пекин");
        cities.put("Shanghai", "Шанхай");
        cities.put("Hong Kong", "Гонконг");
        cities.put("Dubai", "Дубай");
        cities.put("Bangkok", "Бангкок");
        cities.put("Singapore", "Сингапур");
        cities.put("New Delhi", "Нью-Дели");
        cities.put("Mumbai", "Мумбаи");
        cities.put("Jakarta", "Джакарта");
        cities.put("Ho Chi Minh City", "Хошимин");
        cities.put("Manila", "Манила");
        cities.put("Tel Aviv", "Тель-Авив");
        cities.put("Riyadh", "Эр-Рияд");

        // North America
        cities.put("New York", "Нью-Йорк");
        cities.put("Los Angeles", "Лос-Анджелес");
        cities.put("Chicago", "Чикаго");
        cities.put("Houston", "Хьюстон");
        cities.put("Miami", "Майами");
        cities.put("San Francisco", "Сан-Франциско");
        cities.put("Seattle", "Сиэтл");
        cities.put("Toronto", "Торонто");
        cities.put("Montreal", "Монреаль");
        cities.put("Vancouver", "Ванкувер");
        cities.put("Mexico City", "Мехико");
        cities.put("Las Vegas", "Лас-Вегас");

        // South America
        cities.put("Buenos Aires", "Буэнос-Айрес");
        cities.put("Sao Paulo", "Сан-Паулу");
        cities.put("Rio de Janeiro", "Рио-де-Жанейро");
        cities.put("Lima", "Лима");
        cities.put("Bogota", "Богота");
        cities.put("Santiago", "Сантьяго");

        // Africa
        cities.put("Cairo", "Каир");
        cities.put("Cape Town", "Кейптаун");
        cities.put("Johannesburg", "Йоханнесбург");
        cities.put("Nairobi", "Найроби");
        cities.put("Casablanca", "Касабланка");

        // Oceania
        cities.put("Sydney", "Сидней");
        cities.put("Melbourne", "Мельбурн");
        cities.put("Auckland", "Окленд");
        cities.put("Brisbane", "Брисбен");
        cities.put("Perth", "Перт");
        return Collections.unmodifiableMap(cities);
    }
}
