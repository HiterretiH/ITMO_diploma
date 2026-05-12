CUSTOMER = """ООО «Ромашка» (полное наименование: ООО Ромашка полное наименование)
ИНН 7701234567, КПП 770101001, ОГРН 1197746123456
Юр. адрес: 125047, г. Москва, ул. Лесная, д. 5, пом. 12Н
Почтовый адрес: 125047, г. Москва, ул. Лесная, д. 5, пом. 12Н
Р/с 40702810938000012345 в ПАО Сбербанк, БИК 044525225, к/с 30101810400000000225
Тел.: +78120000000, e-mail: buh@romashka.example"""

PERFORMER = """ИП Петров Петр Петрович
ИНН 000000000000, ОГРНИП 320774600456789
Адрес регистрации: 141080, Московская обл., г. Королёв, ул. Калинина, д. 10, кв. 5
Банк: АО Банк
БИК 044525225, р/с 40702810000000000001, к/с 30101810400000000225
КПП 770101001
Тел.: +79001112233"""

DOCX_CONTEXT = {
    "number": "42",
    "date": "2026-05-15",
    "word_date": "15 мая 2026г.",
    "loading_place": "Москва",
    "unloading_place": "Санкт-Петербург",
    "contact_loading": "Контакт погрузки +7 900 123 45 67",
    "contact_unloading": "Контакт разгрузки +7 900 765 43 21",
    "count": "1",
    "price": "98500.00",
    "total_price": "98500.00",
    "word_price": "Девяносто восемь тысяч пятьсот рублей ноль копеек",
    "performer_info_ws": PERFORMER,
    "customer_info_ws": CUSTOMER,
    "performer_name": "ИП Петров",
    "performer_full_name": "ИП Петров Петр Петрович",
    "performer_info": PERFORMER,
    "performer_phone": "+79001112233",
    "performer_bank": "АО Банк",
    "performer_vehicle": "Volvo FH",
    "performer_vehicle_number": "А123ВС178",
    "performer_driver": "Иванов Иван Иванович",
    "performer_driver_phone": "+79002223344",
    "performer_vehicle_type": "тягач с прицепом",
    "performer_inn": "000000000000",
    "performer_bik": "044525225",
    "performer_kpp": "770101001",
    "performer_rsh": "40702810000000000001",
    "performer_ksh": "30101810400000000225",
    "customer_name": "ООО Ромашка",
    "customer_full_name": "ООО Ромашка полное наименование",
    "customer_info": CUSTOMER,
    "customer_phone": "+78120000000",
    "customer_bank": "ПАО Сбербанк, БИК 044525225, р/с 40702810938000012345",
    "orderId": "1001",
    "shipperName": "ООО Ромашка",
    "shipperInn": "7701234567",
    "shipperAddress": CUSTOMER,
    "consigneeName": "ООО «Ромашка», филиал в г. Санкт-Петербург",
    "consigneeInn": "7701234567",
    "consigneeAddress": "196105, г. Санкт-Петербург, ул. Новочеркасская, д. 45, лит. А",
    "cargoDescription": "Сборный груз, упаковка на паллетах, без температурного режима",
    "cargoWeightKg": "12000",
    "routeFrom": "Москва",
    "routeTo": "Санкт-Петербург",
    "loadDate": "2026-05-15",
    "unloadDate": "2026-05-16",
    "driverName": "Иванов Иван Иванович",
    "driverLicense": "77 АА 123456",
    "vehiclePlate": "А123ВС178",
    "vehicleModel": "Volvo FH",
    "vehicleCapacityKg": "20000",
    "priceAmount": "98500.00",
    "currency": "RUB",
}
