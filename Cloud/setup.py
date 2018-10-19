"""
Cloud backend pre projekt Inteligentná domácnosť,
ktorého základ (cloud setup, flask setup, tvorí boilerplate: https://github.com/IBM-Cloud/get-started-python
"""

# Always prefer setuptools over distutils
from setuptools import setup, find_packages
# To use a consistent encoding
from codecs import open
from os import path

here = path.abspath(path.dirname(__file__))

# Get the long description from the README file
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
    name='pma-inteligentna-domacnost',
    version='1.0.0',
    description='Cloudový backend pre projekt Inteligentná domácnosť',
    long_description=long_description,
    url='https://github.com/adamzv/PMA-Inteligentna-domacnost',
    license='Apache-2.0'
)
