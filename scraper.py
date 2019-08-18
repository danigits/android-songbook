#!/usr/bin/env python3
import requests
from lxml import html, etree
import re
import urllib.parse

normal_chord_types = [
	'C',
	'C#/Db',
	'D',
	'D#/Eb',
	'E',
	'F',
	'F#/Gb',
	'G',
	'G#/Ab',
	'A',
	'A#/Bb',
	'B',
]

chord_variation_types = [
	'',
	'maj7',
	'maj9',
	'maj11',
	'maj13',
	'maj9#11',
	'maj13#11',
	'6',
	'add9',
	'6add9',
	'maj7b5',
	'maj7#5',
	'm',
	'm7',
	'm9',
	'm11',
	'm13',
	'm6',
	'madd9',
	'm6add9',
	'mmaj7',
	'mmaj9',
	'm7b5',
	'm7#5',
	'7',
	'9',
	'11',
	'13',
	'7sus4',
	'7b5',
	'7#5',
	'7b9',
	'7#9',
	'7(b5,b9)',
	'7(b5,#9)',
	'7(#5,b9)',
	'7(#5,#9)',
	'9b5',
	'9#5',
	'13#11',
	'13b9',
	'11b9',
	'aug',
	'dim',
	'dim7',
	'5',
	'sus4',
	'sus2',
	'sus2sus4',
	'-5',
]

split_chord_types = [
	'C/E',
	'C/F',
	'C/G',
	'D/F#',
	'D/A',
	'D/Bb',
	'D/B',
	'D/C',
	'E/B',
	'E/C#',
	'E/D',
	'E/D#',
	'E/F',
	'E/F#',
	'E/G',
	'E/G#',
	'Em/B',
	'Em/C#',
	'Em/D',
	'Em/D#',
	'Em/F',
	'Em/F#',
	'Em/G',
	'Em/G#',
	'F/C',
	'F/D',
	'F/D#',
	'F/E',
	'F/G',
	'F/A',
	'Fm/C',
	'G/B',
	'G/D',
	'G/E',
	'G/F',
	'G/F#',
	'A/C#',
	'A/E',
	'A/F',
	'A/F#',
	'A/G',
	'A/G#',
	'Am/C',
	'Am/E',
	'Am/F',
	'Am/F#',
	'Am/G',
	'Am/G#',
]

def build_url(chord_name, variation, version) -> str:
	encoded_chord_name = urllib.parse.quote(chord_name)
	if variation:
		encoded_variation = urllib.parse.quote(variation)
		url = f'http://www.all-guitar-chords.com/index.php?ch={encoded_chord_name}&mm={encoded_variation}&get=Get'
	else:
		url = f'http://www.all-guitar-chords.com/index.php?ch={encoded_chord_name}&get=Get'
	if version:
		url = f'{url}&v={version}'
	return url

def versions_count(chord_name, variation) -> int:
	url = build_url(chord_name, variation, None)
	response = requests.get(url)
	response.raise_for_status()
	tree = html.fromstring(response.content)
	content = etree.tostring(tree, pretty_print=True).decode()
	return content.count('&amp;v=')

def scrape_chord_code(chord_name, variation, version = 1) -> str:
	url = build_url(chord_name, variation, version)
	response = requests.get(url)
	response.raise_for_status()

	tree = html.fromstring(response.content)
	tab = tree.xpath('//table[@id="tab"]')[0]
	tr = tab.findall('tr')[0]
	td = tr.findall('td')[0]
	tds = etree.tostring(td, pretty_print=True).decode()
	content = tds[len('<td>\n'):-len('</td>\n')]
	lines = content.split('<br />')[:-1]
	p = re.compile(r'.*src="img/(.+)\.gif".*')

	frets = []
	for line in lines:
		cfrets = []
		for col in line.split('<img')[1:]:
			match = p.match(col)
			assert match, f'no match for {col}'
			value = match.group(1)
			cfrets.append(value)
		frets.append(cfrets)

	def lastNonEmpty(row) -> int:
		for i in range(len(row) - 1,-1,-1):
			if row[i] != 'empty':
				return i, row[i]
		return -1, None

	subcodes = []
	for row in frets:
		fn, val = lastNonEmpty(row)
		assert fn >= 0
		if val == 'full2':
			subcodes.append('x')
		else:
			subcodes.append(str(fn))

	code = ','.join(subcodes[::-1])

	print(f'{chord_name}{variation}\t,{code}')


# normal chords + variations
for chord in normal_chord_types:
	for variation in chord_variation_types:
		versions = versions_count(chord, variation)
		for version in versions:
			scrape_chord_code(chord, variation, version)

# split chords
for chord in split_chord_types:
	versions = versions_count(chord, None)
	for version in versions:
		scrape_chord_code(chord, None, version)
