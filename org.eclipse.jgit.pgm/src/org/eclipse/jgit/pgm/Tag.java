/*
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008, Charles O'Farrell <charleso@charleso.org>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg.lists@dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, 2020 Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.pgm;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.pgm.internal.CLIText;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@Command(common = true, usage = "usage_CreateATag")
class Tag extends TextBuiltin {
	@Option(name = "-f", usage = "usage_forceReplacingAnExistingTag")
	private boolean force;

	@Option(name = "-d", usage = "usage_tagDelete")
	private boolean delete;

	@Option(name = "--annotate", aliases = {
			"-a" }, usage = "usage_tagAnnotated")
	private boolean annotated;

	@Option(name = "-m", metaVar = "metaVar_message", usage = "usage_tagMessage")
	private String message;

	@Option(name = "--sign", aliases = { "-s" }, forbids = {
			"--no-sign" }, usage = "usage_tagSign")
	private boolean sign;

	@Option(name = "--no-sign", usage = "usage_tagNoSign", forbids = {
			"--sign" })
	private boolean noSign;

	@Option(name = "--local-user", aliases = {
			"-u" }, metaVar = "metaVar_tagLocalUser", usage = "usage_tagLocalUser")
	private String gpgKeyId;

	@Argument(index = 0, metaVar = "metaVar_name")
	private String tagName;

	@Argument(index = 1, metaVar = "metaVar_object")
	private ObjectId object;

	/** {@inheritDoc} */
	@Override
	protected void run() {
		try (Git git = new Git(db)) {
			if (tagName != null) {
				if (delete) {
					List<String> deletedTags = git.tagDelete().setTags(tagName)
							.call();
					if (deletedTags.isEmpty()) {
						throw die(MessageFormat
								.format(CLIText.get().tagNotFound, tagName));
					}
				} else {
					TagCommand command = git.tag().setForceUpdate(force)
							.setMessage(message).setName(tagName);

					if (object != null) {
						try (RevWalk walk = new RevWalk(db)) {
							command.setObjectId(walk.parseAny(object));
						}
					}
					if (noSign) {
						command.setSigned(false);
					} else if (sign) {
						command.setSigned(true);
					}
					if (annotated) {
						command.setAnnotated(true);
					} else if (message == null && !sign && gpgKeyId == null) {
						// None of -a, -m, -s, -u given
						command.setAnnotated(false);
					}
					command.setSigningKey(gpgKeyId);
					try {
						command.call();
					} catch (RefAlreadyExistsException e) {
						throw die(MessageFormat.format(
								CLIText.get().tagAlreadyExists, tagName), e);
					}
				}
			} else {
				ListTagCommand command = git.tagList();
				List<Ref> list = command.call();
				for (Ref ref : list) {
					outw.println(Repository.shortenRefName(ref.getName()));
				}
			}
		} catch (GitAPIException | IOException e) {
			throw die(e.getMessage(), e);
		}
	}
}
