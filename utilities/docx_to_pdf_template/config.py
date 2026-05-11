_REPO_REL = "backend/src/main/resources/templates/documents"

_ACT_FIELDS = [
    {
        "placeholder": "Всего оказано услуг {{ count }}, на сумму {{ total_price }} руб.",
        "field_name": "act_services_total_sentence",
    },
    {
        "placeholder": "Автоуслуги по маршруту: {{ loading_place }} --- {{ unloading_place }}",
        "field_name": "act_route_line",
    },
    {
        "placeholder": "Договор-заявка на перевозку груза №{{ date }} от {{ word_date }}",
        "field_name": "act_contract_ref_line",
    },
    {
        "placeholder": "Акт выполненных работ №{{ number }} от {{ word_date }}",
        "field_name": "act_title_line",
    },
    {"placeholder": "{{ total_price }} руб", "field_name": "total_price_rub"},
    {"placeholder": "{{ performer_info_ws }}", "field_name": "performer_info_ws"},
    {"placeholder": "{{ customer_info_ws }}", "field_name": "customer_info_ws"},
    {"placeholder": "{{ count }}", "field_name": "count"},
    {"placeholder": "{{ price }}", "field_name": "price"},
    {"placeholder": "{{ total_price }}", "field_name": "total_price"},
    {"placeholder": "{{ word_price }}", "field_name": "word_price"},
]

_CONTRACT_FIELDS = [
    {"placeholder": "{{ total_price }} руб. БЕЗ НДС", "field_name": "contract_total_vat_notice"},
    {"placeholder": "ДОГОВОР-ЗАЯВКА № {{ date }}", "field_name": "contract_title_line"},
    {"placeholder": "от {{ word_date }}", "field_name": "contract_word_date_line"},
    {"placeholder": "{{ customer_name }}", "field_name": "customer_name"},
    {"placeholder": "{{ customer_phone }}", "field_name": "customer_phone"},
    {"placeholder": "{{ loading_place }}", "field_name": "loading_place"},
    {"placeholder": "{{ contact_loading }}", "field_name": "contact_loading"},
    {"placeholder": "{{ unloading_place }}", "field_name": "unloading_place"},
    {"placeholder": "{{ contact_unloading }}", "field_name": "contact_unloading"},
    {"placeholder": "{{ performer_vehicle_type }}", "field_name": "performer_vehicle_type"},
    {"placeholder": "{{ performer_vehicle }}", "field_name": "performer_vehicle"},
    {"placeholder": "{{ performer_vehicle_number }}", "field_name": "performer_vehicle_number"},
    {"placeholder": "{{ performer_driver }}", "field_name": "performer_driver"},
    {"placeholder": "{{ performer_driver_phone }}", "field_name": "performer_driver_phone"},
    {"placeholder": "{{ performer_info }}", "field_name": "performer_info"},
    {"placeholder": "{{ customer_info }}", "field_name": "customer_info"},
]

_WAYBILL_FIELDS = [
    {
        "placeholder": "Всего наименований {{ count }}, на сумму {{ total_price }} руб",
        "field_name": "waybill_items_total_line",
    },
    {
        "placeholder": "Автоуслуги по маршруту: {{ loading_place }} --- {{ unloading_place }}",
        "field_name": "waybill_route_line",
    },
    {
        "placeholder": "Договор-заявка на перевозку груза №{{ date }} от {{ word_date }}",
        "field_name": "waybill_contract_ref_line",
    },
    {
        "placeholder": "Счет на оплату №{{ number }} от {{ word_date }}",
        "field_name": "waybill_invoice_title",
    },
    {"placeholder": "{{ total_price }} руб", "field_name": "waybill_total_price_rub"},
    {"placeholder": "{{ performer_bank }}", "field_name": "performer_bank"},
    {"placeholder": "{{ performer_bik }}", "field_name": "performer_bik"},
    {"placeholder": "{{ performer_ksh }}", "field_name": "performer_ksh"},
    {"placeholder": "{{ performer_inn }}", "field_name": "performer_inn"},
    {"placeholder": "{{ performer_kpp }}", "field_name": "performer_kpp"},
    {"placeholder": "{{ performer_rsh }}", "field_name": "performer_rsh"},
    {"placeholder": "{{ performer_full_name }}", "field_name": "performer_full_name"},
    {"placeholder": "{{ performer_info_ws }}", "field_name": "performer_info_ws"},
    {"placeholder": "{{ customer_info_ws }}", "field_name": "customer_info_ws"},
    {"placeholder": "{{ count }}", "field_name": "count"},
    {"placeholder": "{{ price }}", "field_name": "price"},
    {"placeholder": "{{ total_price }}", "field_name": "total_price"},
    {"placeholder": "{{ word_price }}", "field_name": "word_price"},
]

JOBS = [
    {
        "input": f"{_REPO_REL}/act_of_work.docx",
        "output": f"{_REPO_REL}/act_of_work.form.docx",
        "fields": _ACT_FIELDS,
    },
    {
        "input": f"{_REPO_REL}/contract_application.docx",
        "output": f"{_REPO_REL}/contract_application.form.docx",
        "fields": _CONTRACT_FIELDS,
    },
    {
        "input": f"{_REPO_REL}/waybill.docx",
        "output": f"{_REPO_REL}/waybill.form.docx",
        "fields": _WAYBILL_FIELDS,
    },
]
