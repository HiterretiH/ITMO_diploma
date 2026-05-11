INPUT_FILE = "templates/CONTRACT_APPLICATION.docx"
OUTPUT_FILE = "templates/CONTRACT_APPLICATION_with_fields.docx"

FIELDS = [
    {"placeholder": "{{ orderNumber }}", "field_name": "orderNumber", "default_text": ""},
    {"placeholder": "{{ date }}", "field_name": "date"},
    {"placeholder": "{{ companyName }}", "field_name": "companyName"},
]
