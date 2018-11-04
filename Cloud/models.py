from datetime import datetime
from app import db
from peewee import *


class BaseModel(Model):
    class Meta:
        database = db


class Dht(BaseModel):
    device_id = TextField()
    cas = DateTimeField(default=datetime.now)
    teplota = FloatField()
    vlhkost = FloatField()


class Senzor(BaseModel):
    device_id = TextField()
    typ_senzoru = TextField()
    miestnost = IntegerField(default=None)
    status = TextField(default=None)
