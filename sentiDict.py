from openpyxl import load_workbook

train_set = load_workbook(filename=r'train_set.xlsx')

sheets = train_set.get_sheet_names()
sheet0 = sheets[0]
worksheet = train_set.get_sheet_by_name(sheet0)

rows = list(worksheet.rows)

sentimentDict = {}

rows = rows[1:]
for row in rows:
    line = [col.value for col in row]

    if not line[3]:
        continue

    keys = line[3].split(';')
    values = line[4].split(';')

    keys.pop()
    values.pop()

    itr = 0
    for itr in range(len(keys)):
        key = keys[itr]
        value = values[itr]
        sentimentDict[key] = value

dictFile = open('sentimentDict.csv', "w", encoding="utf8")

keys = sentimentDict.keys()
for key in keys:
    value = sentimentDict.get(key)
    dictFile.write(key)
    dictFile.write(",")
    dictFile.write(value)
    dictFile.write("\n")

dictFile.close()
