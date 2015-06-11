from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

import socket, sys, json, logging, os
from os import path
from copy import copy

#sys.path.append('/opt/marcopolo/')
from marcopolo.marco_conf import utils, conf

from marcopolo.marco.marco import Marco, MarcoException

class MarcoBinding(DatagramProtocol):
	"""
	Twisted class for an asynchronous socket server
	"""
	def __init__(self):
		self.marco = Marco() #Own instance of Marco

	def __del__(self):
		del self.marco
	
	def startProtocol(self):
		logging.info("Starting service marcod")

	def marcoInThread(self, command, address):
		nodes = []
		
		nodes = self.marco.marco(max_nodes=command.get("max_nodes", None), 
								 exclude=command.get("exclude", []),
								 timeout=command.get("timeout", None),
								 params=command.get("params", {}),
								 group=command.get("group", conf.MULTICAST_ADDR)
								 )

		self.transport.write(bytes(json.dumps([{"Address":n.address, "Params": n.params} for n in nodes]).encode('utf-8')), address)

	def requestForInThread(self, command, address):
		nodes = self.marco.request_for(command["Params"],
										max_nodes=command.get("max_nodes", None),
										exclude=command.get("exclude", []),
										params=command.get("params", {}),
										timeout=command.get("timeout", None))
		if len(nodes) > 0:
			self.transport.write(bytes(json.dumps([{"Address": n.address, "Params": n.params} for n in nodes]).encode('utf-8')), address)
		else:
			self.transport.write(bytes(json.dumps([]), 'utf-8'), address)
	
	def servicesInThread(self, command, address):
		services = self.marco.services(addr=command.get("node", None), 
									   timeout=command.get("timeout", 0)
									   )
		
		self.transport.write(bytes(json.dumps([service for service in services]).encode('utf-8')), address)
	
	def datagramReceived(self, data, address):
		print("Datagram received")
		print(data)
		try:
			command = json.loads(data.decode('utf-8'))
		except ValueError:
			return
		if command.get("Command", None) == None:
			self.transport.write(bytes(json.dumps({"Error": True}).encode('utf-8')), address)

		else:
			if command["Command"] == "Marco":
				reactor.callInThread(self.marcoInThread, command, address)

			elif command["Command"] == "Request-for" or command["Command"] == "Request-For":
				reactor.callInThread(self.requestForInThread, command, address)

			elif command["Command"] == "Services":
				reactor.callInThread(self.servicesInThread, command, address)
			
			else:
				self.transport.write(bytes(json.dumps({"Error": True}).encode('utf-8')), address)
		